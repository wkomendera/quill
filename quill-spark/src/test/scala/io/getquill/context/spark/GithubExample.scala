package io.getquill.context.spark

import java.io.File
import java.net.URL

import scala.language.postfixOps
import scala.sys.process._

import org.apache.spark.sql.SparkSession

import io.getquill.QuillSparkContext.Ord
import io.getquill.QuillSparkContext._

case class User(
  id: String,
  login: String,
  gravatar_id: String,
  url: String,
  avatar_url: String)

case class Repo(
  id: String,
  name: String,
  url: String)

case class Activity(
  id: String,
  `type`: String,
  actor: User,
  repo: Repo,
  created_at: String,
  org: User)

object Playground extends App {

  implicit val sqlContext =
    SparkSession
      .builder()
      .master("local[*]")
      .appName("spark test")
      .getOrCreate()
      .sqlContext
  //tweets
  //  .select('text.as[String])   // select the text column (Dataframe)
  //  .flatMap(_.split("\\s+"))   // split it into words    (Dataset)
  //  .filter(_.startsWith("#"))  // filter hashtag words    (Dataset)
  //  .map(_.toLowerCase)         // normalize hashtags     (Dataset)
  //  .groupBy('value)            // group by each word     (Dataframe)
  //  .agg(count("*") as "count") // aggregate the count    (Dataframe)
  //  .orderBy('count desc)       // order                  (Datafeame)
  //  .limit(topCount)            // limit to top results   (Dataframe) 
  //  .as[(String, BigInt)]       // set the type again     (Dataset)

  case class Tweet(text: String)

  import sqlContext.implicits._

  val tweets = List(Tweet("some #hashtag #h2"), Tweet("dds #h2")).toQuery //.toDS().createOrReplaceTempView("tweets")

  def explode[T] = quote {
    (a: Array[T]) => infix"explode(${a})".as[T]
  }

  println {
    run {
      tweets.map { tweet =>
        explode(tweet.text.split(" "))
      }.nested.filter(_.startsWith("#"))
    }.show
  }

  //  sqlContext.sql("SELECT lower(w.w), count(*) FROM (SELECT EXPLODE(SPLIT(t.text, ' ')) w FROM tweets t) w where w.w like '#%' group by lower(w.w) sort by count(*) desc limit 100").show
}

//object GithubExample extends App {
//
//  val files =
//    for {
//      year <- 2017 to 2017
//      month <- 10 to 10
//      day <- 22 to 22
//      hour <- 0 to 23
//    } yield "%04d-%02d-%02d-%02d".format(year, month, day, hour)
//
//  files.par.foreach { name =>
//    val file = new File(s"$name.json.gz")
//    if (!file.exists()) {
//      println(s"downloading missing file $name")
//      new URL(s"http://data.githubarchive.org/$name.json.gz") #> new File(s"$name.json.gz") !!
//    }
//  }
//
//  implicit val sqlContext =
//    SparkSession
//      .builder()
//      .master("local[*]")
//      .appName("spark test")
//      .getOrCreate()
//      .sqlContext
//
//  import sqlContext.implicits._
//
//  val activities = sqlContext.read.json(files.map(n => s"$n.json.gz"): _*).as[Activity].toQuery
//
//  val topStargazers = quote {
//    activities
//      .groupBy(_.actor)
//      .map {
//        case (actor, list) => (actor.login, list.size)
//      }.sortBy {
//        case (login, size) => size
//      }(Ord.desc)
//  }
//
//  val topProjects = quote {
//    activities
//      .filter(_.`type` == "WatchEvent")
//      .groupBy(_.repo)
//      .map {
//        case (repo, list) => (repo.name, list.size)
//      }.sortBy {
//        case (repoName, size) => size
//      }(Ord.desc)
//  }
//
//  println(run(topStargazers).show())
//  println(run(topProjects).show())
//}