package server
import akka.actor.{ Actor, ActorRef, Props, ActorSystem }
import akka.actor.actorRef2Scala
import akka.dispatch.ExecutionContexts.global
import akka.pattern.ask
import scala.concurrent.duration.DurationInt
import scala.io.Source.fromFile
import java.security.MessageDigest
import akka.routing.RoundRobinRouter
import java.util.Random
import akka.util.Timeout

case class BitCoinMiningStart()
case class TotalTrials()
case class ProcessMining(k:Integer,gatorID:String)
case class BitCoinsByServer(bitcoin: String, validShahashvalue: String)

object EncryptSha extends App{
  val system= ActorSystem("MainSystem")
  println("\n\nEnter 'k' - the required number of leading zeroes : ")
  var k= readInt
  val serveract= system.actorOf(Props[Server],"Server")
  val Mining=system.actorOf(Props (new BitCoinMining(k)))
  implicit val timeout = Timeout(25 seconds)
  val response= Mining ? BitCoinMiningStart()
}

class Server extends Actor{
  def receive={
    case msg: String=>
      println("Inside Server: Recieved from Client "+msg)
  }
}

class BitCoinMining(k: Integer) extends Actor{
  var worker=context.actorFor("akka://WorkerSystem@127.0.0.1:5153/user/Client")
 private var servertrycount=0
  def receive={
    case msg: String=>
      println("Inside server BitCoinMining: ")

    case BitCoinMiningStart()=>{
      println("Server BitcoinMiningStart")
      var gatorID:String="souravkparmar"
      var actorcount = 1
      var remotestr=k+gatorID
      var miner=context.actorOf(Props[CheckBitcoins].withRouter(RoundRobinRouter(actorcount)))
 for(i<-0 to 1000000)
{
       miner ! ProcessMining(k,gatorID)
       worker ! remotestr
}
    }
    case TotalTrials()=>{
    //  println("\ntotaltrials")
      servertrycount=servertrycount+1
     if(servertrycount>=10000)
         context.system.shutdown()
}

    case BitCoinsByServer( bitcoin: String, validShahashvalue: String)=>{
     
      println("From server Bitcoin : " + bitcoin + "     "+ validShahashvalue)
      
      
    }
    case _ =>"Error Case"
  }
}
/*Calculates Sha256 for server and returns Msg to server */
class CheckBitcoins extends Actor{
  def receive={
    case ProcessMining(k,gatorID) =>{
      val fun = scala.util.Random.alphanumeric.take(10).mkString+" "
      val bitcoin = gatorID+""+fun
      val tobehashed=bitcoin
      var sha = MessageDigest.getInstance("SHA-256").digest(tobehashed.getBytes("UTF-8"))
      var res= sha.map("%02x".format(_)).mkString
      val hashstring= new StringBuffer()
      //println("\n ProcessMining "+res)
      hashstring.append(res)
      sender ! TotalTrials()
      var count=0
      for (i<-0 to k-1){
        if(hashstring.toString.charAt(i)=='0')
          count=count+1
        if(count==k) {
         // println("CheckBitCoin Server \n")
          sender ! BitCoinsByServer(bitcoin, res)
        }
      }
    }
    case _ =>"Checkbit invalid case"
  }


}
