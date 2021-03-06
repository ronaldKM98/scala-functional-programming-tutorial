package co.edu.eafit.dis.progfun.catscases.asynctesting

import cats.{Applicative, Id}

import cats.implicits._
import scala.language.higherKinds

object TestingAsynchronousCode extends App {
  trait UptimeClient[F[_]] {
    def getUptime(hostname: String): F[Int]
  }

  // Production env
  class UptimeService[F[_]] (client: UptimeClient[F])
                            (implicit val applicative: Applicative[F]) {
    def getTotalUptime(hostnames: List[String]): F[Int] =
      hostnames.traverse(client.getUptime).map(_.sum)
  }

  //Test env. Synchronous
  class TestUptimeClient(hosts: Map[String, Int]) extends UptimeClient[Id] {
    def getUptime(hostname: String): Id[Int] =
      hosts.getOrElse(hostname, 0)
  }

  def testTotalUptime(): Unit = {
    val hosts = Map("host1" -> 10, "host2" -> 6)
    val client = new TestUptimeClient(hosts)
    val service = new UptimeService(client)
    val actual = service.getTotalUptime(hosts.keys.toList)
    val expected = hosts.values.sum
    print(s"Actual == Expected: $actual == $expected")
    assert(actual == expected)
  }

  testTotalUptime()
}
