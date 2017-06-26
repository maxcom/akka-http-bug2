import java.io.FileInputStream
import java.security.{KeyStore, SecureRandom}
import javax.net.ssl.{KeyManagerFactory, SSLContext}

import akka.actor.ActorSystem
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.{ConnectionContext, Http}
import akka.stream.ActorMaterializer
import akka.util.ByteString
import org.asynchttpclient.Dsl
import org.asynchttpclient.Dsl._
import org.asynchttpclient.request.body.multipart.ByteArrayPart

import scala.concurrent.Future
import scala.util.{Failure, Success}

object Boot extends App {
  implicit val system = ActorSystem("abook-slave")
  implicit val materializer = ActorMaterializer()
  implicit val executionContext = system.dispatcher

  val mySSLContext = {
    val ks = KeyStore.getInstance("JKS")
    val ksIs = new FileInputStream("keystore.jks")
    val password = "qwerty"
    try {
      ks.load(ksIs, password.toCharArray)
    } finally {
      if (ks != null) {
        ksIs.close()
      }
    }
    val kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm)
    kmf.init(ks, password.toCharArray)

    val context = SSLContext.getInstance("TLSv1.2")
    context.init(kmf.getKeyManagers, null, new SecureRandom())
    context
  }

  def startService(): Future[Http.ServerBinding] = {
    val routes =
      fileUpload("first") {
        case (_, bytes) ⇒
          val allBytesF = bytes.runFold(ByteString.empty) { (all, bytes) ⇒ all ++ bytes }

          onSuccess(allBytesF) { _ ⇒
            complete("ok")
          }
      }

    Http().bindAndHandle(routes, "0.0.0.0", 8080, ConnectionContext.https(mySSLContext))
  }

  startService().onComplete {
    case Success(binding) ⇒
      try {
        system.log.info(s"Service started at ${binding.localAddress}")

        val req = Dsl.post("https://localhost:8080/")

        req.addBodyPart(new ByteArrayPart("first", Array.fill(100000)('a'), null, null, "first.name"))

        // it works fine when you comment out this line
        req.addBodyPart(new ByteArrayPart("second", Array.fill(100000)('a'), null, null, "second.name"))

        val client = asyncHttpClient(config().setAcceptAnyCertificate(true))

        val response = client.executeRequest(req.build()).get()

        println("Status " + response.getStatusCode)

        if (response.getStatusCode != 200) {
          println(response.getResponseBody)
        }
      } catch {
        case ex: Throwable ⇒
          ex.printStackTrace()
      } finally {
        System.exit(0)
      }
    case Failure(e) ⇒
      system.log.error(e, s"Failed to bind")
      system.terminate()
      System.exit(0)
  }
}