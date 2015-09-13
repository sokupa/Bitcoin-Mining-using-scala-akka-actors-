import akka.actor.{ Actor, ActorRef, Props, ActorSystem }
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



case class serverIp(serverip: String)
case class Pollwork(whichWorker: String)
//case class ProcessMining(start: Int, nrOfElements: Int,noOfZeroes: Int)
case class Result(resultmap: HashMap[String,String] , whichWorker: String)
case class startwork(begin: Int,end: Int, numberofZeroes :Int,whichworker: String)
case class serverstart()



object Driver extends App{
 if(args(0).split("\\.").length == 4){
    val worker = ActorSystem("WorkerSystem").actorOf(Props[Worker].withRouter(RoundRobinRouter(8)))
   for (i<-1 to 8)
      worker ! serverIp(args(0))
 }
  else
{
   val server =ActorSystem("ServerSystem").actorOf(Props (new Server(args(0).toInt)),name="Server")
   server! serverstart()
}
}

class Server(numberofZeroes: Int) extends Actor{
     var totalwork=1000000
     var workersremote = 0
     var serverworker =8
     var totalcoin = 0
     var currentCointcount = 0
     var finishedworkercount: Int = 1
     var Finalmap = new HashMap[String , String]
     var start: Long = _
     var inputprocessed = 0
    def receive = {
      case serverstart()=>{
      val server_worker = context.actorOf(Props[Worker].withRouter(RoundRobinRouter(nrOfInstances = serverworker)))
      println("server start \n")
      for(i<-1 to serverworker)
        {
          server_worker ! startwork(i * 10000, 10000*(i+1),numberofZeroes,"Server")
          start = System.currentTimeMillis()
        }

  }
    case Pollwork(whichWorker)=>{
      println("pollwork start \n")
      if(whichWorker.equalsIgnoreCase("remoteworker"))
          workersremote = workersremote+1
      sender ! startwork(1,10000,numberofZeroes,"remoteworker")
  }
      case Result(resultmap , whichworker)=>{
     println("server Result \n")
        currentCointcount = resultmap.size
        totalcoin += currentCointcount
        inputprocessed += 10000
       totalwork=totalwork-10000
        println("current worker"+whichworker+"vaild coin"+currentCointcount)
        resultmap.foreach {keyVal => println(keyVal._1 + "\t" + keyVal._2)} 
        resultmap.foreach{keyVal => Finalmap.put (keyVal._1, keyVal._2)}
       // if((System.currentTimeMillis()-start)<30000){
         if(totalwork >0){
          sender ! startwork(1,10000,numberofZeroes,whichworker)
        }else{
          if(finishedworkercount ==(workersremote + serverworker) ){
            Finalmap.foreach{keyVal => println(keyVal._1 + " " + keyVal._2)}
            println("Total number of input processed:" +inputprocessed)
            println("\n Number of Bitcoins found : %s\nTotal time taken : %s millis".format(totalcoin, (System.currentTimeMillis - start)))
            context.stop(self)
          }
          else{
            finishedworkercount=finishedworkercount+1
          }
        }
      }
    case _ =>
  }


}

class Worker extends Actor{

  def receive = {
    case serverIp(serverip)=>{
      println("Logtag_Worker: Server IP"+serverip)
      val RoutetoServer = context.actorFor("akka://ServerSystem@"+serverip+":5155/user/Server")
        RoutetoServer ! Pollwork("remoteworker")
  }
    case startwork(start, nrOfElements, noOfZeroes,whichworker)=>{
      //val MineCoin = context.actorOf(Props[CheckBitcoins], "CheckBitCoins")
        println("worker  startwork \n")
        sender ! Result (ProcessMining(start, nrOfElements,noOfZeroes),whichworker) 
    }

  }

    def ProcessMining(start: Int,nrofElements: Int,k: Int): HashMap [String, String]={
      val gatorID = "souravkparmar"
      var bitcoinmap = new HashMap[String, String]
      var teststring=""
      var sha=""
      println("ProcessMining enter \n")
      for (i <- start until nrofElements) {
        teststring = gatorID+scala.util.Random.alphanumeric.take(15).mkString
        sha = MessageDigest.getInstance("SHA-256").digest(teststring.getBytes("UTF-8")).map("%02x".format(_)).mkString
       // println("ProcessMining"+ sha+" hashstring"+ teststring)
        //val hashstring = new StringBuffer()
        //hashstring.append(sha)
        //var count = 0
        if(sha.matches("0{"+k+"}[a-zA-Z0-9]*")){
          // println("Match"+teststring+" "+ sha)
           val result= bitcoinmap.put(teststring, sha)
             }
        }
    bitcoinmap
 }
}





