/**************************************************************************************************************
*************************BitCoin Mining using scala************************************************************
*Instuctions to Run: 
*As server: sbt "project server" "run 3" **** run sbt on server directory
*As client: sbt "project client" "192.108.0.13" Replace it with source ip **** run sbt on client directory******
*use application.conf to change server ip or client Ip 
*Publisher: Souav kumar parmar 
*  	    Priyanshu Pandey
****************************************************************************************************************/
import akka.actor.{Actor, ActorRef, Props, ActorSystem}
import akka.actor.actorRef2Scala
import akka.dispatch.ExecutionContexts.global
import akka.pattern.ask
import scala.collection.immutable
import scala.collection.mutable.HashMap
import scala.concurrent.duration.DurationInt
import scala.io.Source.fromFile
import java.security.MessageDigest
import akka.routing.RoundRobinRouter
import java.util.Random
import akka.util.Timeout
import java.io._

case class serverIp(serverip: String)

case class Pollwork(whichWorker: String)

case class Result(resultmap: HashMap[String, String], whichWorker: String)

case class startwork(begin: Int, end: Int, numberofZeroes: Int, whichworker: String)

case class serverstart()

/********************************************************************************************
*Class Server: Works as master worker 
*    serverstart(): Alloacte works to server worker
*    Pollwork(): schedule work to whichworker
*    Result(): Publishes result calculated by server and remote client
 ********************************************************************************************/
class Server(numberofZeroes: Int) extends Actor {
  var totaltries = 10000
  var worksize = 10000
  var workersremote = 0
  var serverworker = (Runtime.getRuntime().availableProcessors()) * 2
  var totalcoin = 0
  var currentCointcount = 0
  var finishedworkercount: Int = 1
  var Finalmap = new HashMap[String, String]
  var start: Long = _
  var inputprocessed = 0

  def receive = {
    case serverstart() => {
      val server_worker = context.actorOf(Props[Worker].withRouter(RoundRobinRouter(nrOfInstances = serverworker)))
      println("server start \n")
      start = System.currentTimeMillis()
      for (i <- 1 to serverworker) {
        server_worker ! startwork(1, worksize, numberofZeroes, "Server")
      }

    }
    case Pollwork(whichWorker) => {
      println("pollwork start \n")
      if (whichWorker.equalsIgnoreCase("remoteworker"))
        workersremote = workersremote + 1
      sender ! startwork(1, worksize, numberofZeroes, "remoteworker")
    }
    case Result(resultmap, whichworker) => {
      currentCointcount = resultmap.size
      totalcoin += currentCointcount
      inputprocessed += worksize
      totaltries = totaltries - 1
      println("current worker " + whichworker + " vaild Bit coin count " + currentCointcount)
      resultmap.foreach { keyVal => println(keyVal._1 + "\t" + keyVal._2) }
      resultmap.foreach { keyVal => Finalmap.put(keyVal._1, keyVal._2) }
      if (totaltries > 0) {
        sender ! startwork(1, worksize, numberofZeroes, whichworker)
      }
      else {
        if (finishedworkercount == (workersremote + serverworker)) {
          Finalmap.foreach { keyVal => println(keyVal._1 + " " + keyVal._2) }
          println("Total number of input processed: " + inputprocessed)
          println("\n Number of Bitcoins found : %s\nTotal time taken : %s millis".format(totalcoin, (System.currentTimeMillis - start)))
          context.stop(self)
        }
        else {
          finishedworkercount = finishedworkercount + 1
        }
      }
    }
    case _ =>
  }


}
/***************************************************************************************************************
* Class Worker: Member of ActorSystem WorkerSystem : Calculates Hash of inputstring
* serverIp: Connects to server with serverIp
* startwork: Mines bit coin
* function ProcessMining: Check for valid bit coin
****************************************************************************************************************/
class Worker extends Actor {

  def receive = {
    case serverIp(serverip) => {
      println("Logtag_Worker: Server IP" + serverip)
      val RoutetoServer = context.actorFor("akka://ServerSystem@" + serverip + ":5155/user/Server")
      RoutetoServer ! Pollwork("remoteworker")
    }
    case startwork(start, nrOfElements, noOfZeroes, whichworker) => {
      println("worker  startwork \n")
      sender ! Result(ProcessMining(start, nrOfElements, noOfZeroes), whichworker)
    }

  }

  def ProcessMining(start: Int, nrofElements: Int, k: Int): HashMap[String, String] = {
    val gatorID = "souravkparmar"
    var bitcoinmap = new HashMap[String, String]
    var teststring = ""
    var sha = ""
    var Lastsucessfulhash = "axysads" /*inital string*/
    println("ProcessMining enter \n")
    for (i <- start until nrofElements) {
      teststring = gatorID + scala.util.Random.alphanumeric.take(10).mkString + Lastsucessfulhash.substring(0, 7)
      sha = MessageDigest.getInstance("SHA-256").digest(teststring.getBytes("UTF-8")).map("%02x".format(_)).mkString
      if (sha.matches("0{" + k + "}[a-zA-Z0-9]*")) {
        val result = bitcoinmap.put(teststring, sha)
        Lastsucessfulhash = sha
      }
    }
    bitcoinmap
  }
}


object Driver extends App {
  if (args(0).split("\\.").length == 4) {
    val process = Runtime.getRuntime().availableProcessors()
    val workers = (process) * 3
    val worker = ActorSystem("WorkerSystem").actorOf(Props[Worker].withRouter(RoundRobinRouter(workers)))
    println("Process \n" + process)
    for (i <- 1 to 8)
      worker ! serverIp(args(0))
  }
  else {
    val server = ActorSystem("ServerSystem").actorOf(Props(new Server(args(0).toInt)), name = "Server")
    server ! serverstart()
  }
}
