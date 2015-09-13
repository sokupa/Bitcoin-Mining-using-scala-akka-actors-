package Worker

import akka.actor.{ Actor, ActorRef, Props, ActorSystem }
import akka.actor.actorRef2Scala
import akka.dispatch.ExecutionContexts.global
import akka.pattern.ask
import scala.concurrent.duration.DurationInt
import scala.io.Source.fromFile
import java.security.MessageDigest
import akka.routing.RoundRobinRouter
import java.util.Random

case class  ClientProcessCount()
case class ProcessMining(k: Integer, gatorId: String)

object worker extends App{
  val system=ActorSystem("WorkerSystem")
  val worker=system.actorOf(Props[Client],"Client")
}

class Client extends Actor{
  private var clienttry = 0
  def receive={
    case msg: String=>{
      println("Inside client: Received from server"+msg)
      val clientworkercount=5
      val clientworker=context.actorOf(Props[CheckBitcoins].withRouter(RoundRobinRouter(clientworkercount)))
      val k: Integer=msg.charAt(0)-'0'
      val gatorID="souravkparmar"
      sender ! (clientworker ! ProcessMining(k,gatorID))}

      case ClientProcessCount() =>{
        clienttry = clienttry + 1
      if(clienttry>=100000)
         context.system.shutdown()
      }


    case _ =>
  }

}

class CheckBitcoins extends Actor {
  var Toserver = context.actorFor("akka://MainSystem@127.0.0.1:5151/user/Server")

  def receive = {
    case ProcessMining(k, gatorID) => {
      val fun = scala.util.Random.alphanumeric.take(10).mkString + " "
      val bitcoin = gatorID + "" + fun
      val tobehashed = bitcoin
      var sha = MessageDigest.getInstance("SHA-256").digest(tobehashed.getBytes("UTF-8"))
      var res = sha.map("%02x".format(_)).mkString
      var count1 =1
      if(count1 == 1)
        {
         println("Client Process"+res)
         count1 =0
        }
      val hashstring = new StringBuffer()
     // println("\n Client ProcessMining " + res)
      hashstring.append(res)
      sender ! ClientProcessCount()
      var count = 0
      for (i <- 0 to k - 1) {
        if (hashstring.toString.charAt(i) == '0')
          count = count + 1
        if (count == k) {
          //println("CheckBitCoin Client \n")
          val resultstr = bitcoin + " Client" + res
          println("Client Process"+resultstr)
          Toserver ! resultstr
        }
      }
    }
    case _ => "Checkbit invalid case"
  }
}
