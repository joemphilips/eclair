package fr.acinq.eclair.db

import java.net.{InetAddress, InetSocketAddress}
import java.sql.DriverManager

import fr.acinq.bitcoin.{Block, Crypto}
import fr.acinq.eclair.db.sqlite.SqliteNetworkDb
import fr.acinq.eclair.randomKey
import fr.acinq.eclair.router.Announcements
import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner
import org.sqlite.SQLiteException

@RunWith(classOf[JUnitRunner])
class SqliteNetworkDbSpec extends FunSuite {

  def inmem = DriverManager.getConnection("jdbc:sqlite::memory:")

  test("init sqlite 2 times in a row") {
    val sqlite = inmem
    val db1 = new SqliteNetworkDb(sqlite)
    val db2 = new SqliteNetworkDb(sqlite)
  }

  test("add/remove/list nodes") {
    val sqlite = inmem
    val db = new SqliteNetworkDb(sqlite)

    val node_1 = Announcements.makeNodeAnnouncement(randomKey, "node-alice", (100.toByte, 200.toByte, 300.toByte), new InetSocketAddress(InetAddress.getByAddress(Array[Byte](192.toByte, 168.toByte, 1.toByte, 42.toByte)), 42000) :: Nil)
    val node_2 = Announcements.makeNodeAnnouncement(randomKey, "node-bob", (100.toByte, 200.toByte, 300.toByte), new InetSocketAddress(InetAddress.getByAddress(Array[Byte](192.toByte, 168.toByte, 1.toByte, 42.toByte)), 42000) :: Nil)
    val node_3 = Announcements.makeNodeAnnouncement(randomKey, "node-charlie", (100.toByte, 200.toByte, 300.toByte), new InetSocketAddress(InetAddress.getByAddress(Array[Byte](192.toByte, 168.toByte, 1.toByte, 42.toByte)), 42000) :: Nil)

    assert(db.listNodes().toSet === Set.empty)
    db.addNode(node_1)
    db.addNode(node_2)
    db.addNode(node_3)
    assert(db.listNodes().toSet === Set(node_1, node_2, node_3))
    db.removeNode(node_2.nodeId)
    assert(db.listNodes().toSet === Set(node_1, node_3))
    db.updateNode(node_1)
  }

  test("add/remove/list channels and channel_updates") {
    val sqlite = inmem
    val db = new SqliteNetworkDb(sqlite)

    def sig = Crypto.encodeSignature(Crypto.sign(randomKey.toBin, randomKey)) :+ 1.toByte

    val channel_1 = Announcements.makeChannelAnnouncement(Block.RegtestGenesisBlock.hash, 42, randomKey.publicKey, randomKey.publicKey, randomKey.publicKey, randomKey.publicKey, sig, sig, sig, sig)
    val channel_2 = Announcements.makeChannelAnnouncement(Block.RegtestGenesisBlock.hash, 43, randomKey.publicKey, randomKey.publicKey, randomKey.publicKey, randomKey.publicKey, sig, sig, sig, sig)
    val channel_3 = Announcements.makeChannelAnnouncement(Block.RegtestGenesisBlock.hash, 44, randomKey.publicKey, randomKey.publicKey, randomKey.publicKey, randomKey.publicKey, sig, sig, sig, sig)

    assert(db.listChannels().toSet === Set.empty)
    db.addChannel(channel_1)
    db.addChannel(channel_2)
    db.addChannel(channel_3)
    assert(db.listChannels().toSet === Set(channel_1, channel_2, channel_3))
    db.removeChannel(channel_2.shortChannelId)
    assert(db.listChannels().toSet === Set(channel_1, channel_3))

    val channel_update_1 = Announcements.makeChannelUpdate(Block.RegtestGenesisBlock.hash, randomKey, randomKey.publicKey, 42, 5, 7000000, 50000, 100, true)
    val channel_update_2 = Announcements.makeChannelUpdate(Block.RegtestGenesisBlock.hash, randomKey, randomKey.publicKey, 43, 5, 7000000, 50000, 100, true)
    val channel_update_3 = Announcements.makeChannelUpdate(Block.RegtestGenesisBlock.hash, randomKey, randomKey.publicKey, 44, 5, 7000000, 50000, 100, true)

    assert(db.listChannelUpdates().toSet === Set.empty)
    db.addChannelUpdate(channel_update_1)
    intercept[SQLiteException](db.addChannelUpdate(channel_update_2))
    db.addChannelUpdate(channel_update_3)
    db.removeChannel(channel_3.shortChannelId)
    assert(db.listChannels().toSet === Set(channel_1))
    assert(db.listChannelUpdates().toSet === Set(channel_update_1))
    db.updateChannelUpdate(channel_update_1)
  }

}
