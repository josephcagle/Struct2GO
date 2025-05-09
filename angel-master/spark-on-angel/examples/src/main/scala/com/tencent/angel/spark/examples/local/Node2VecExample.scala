package com.tencent.angel.spark.examples.local

import com.tencent.angel.graph.embedding.node2vec.Node2Vec
import com.tencent.angel.graph.utils.GraphIO
import com.tencent.angel.spark.examples.local.dir.getFile
import org.apache.spark.sql.SparkSession
import org.apache.spark.storage.StorageLevel

import java.io.File


object Node2VecExample {

  def main(args: Array[String]): Unit = {
    // Use Struct2GO edge list directory
    //      val path = new File("../../../data/proteins_edgs")
    val path = new File("data/proteins_edgs")
    for (file <- path.listFiles()) {
      println(file.getName)
    }
//    println(System.getProperty("user.dir"))

    // Spark setup
    val mode = "local"
    val spark = SparkSession.builder()
      .master(mode)
      .appName("node2vec")
      .getOrCreate()

//    for(name <- getFile(path)) {
    timeLoop(getFile(path), (x: File) => x.toString) { name =>
      println(name)
      val names1 = name.toString.split("/")
      val input = if (args.length > 0) args(0) else name.toString
      // Output to node2vec directory, preserving filename
      val output = if (args.length > 1) args(1) else "data/node2vec/" + names1.last.replace(".txt", "")
      val batchSize = if (args.length > 2) args(2).toInt else 128
      val psPartNum = if (args.length > 3) args(3).toInt else 2
      val dataPartNum = if (args.length > 4) args(4).toInt else 1
      val storageLevel = if (args.length > 5) StorageLevel.fromString(args(5)) else StorageLevel.MEMORY_ONLY
      val delimiter = if (args.length > 6) args(6) match {
        case "space" => " "
        case "comma" => ","
        case "tab" => "\t"
      } else " "
      val isWeighted = if (args.length > 7) args(7).toBoolean else false
      val walkLength = if (args.length > 8) args(8).toInt else 30
      val pValue = if (args.length > 9) args(9).toDouble else 0.8
      val qValue = if (args.length > 10) args(10).toDouble else 1.2
      val needReplicaEdge = if (args.length > 11) args(11).toBoolean else false
      val useTrunc = if (args.length > 12) args(12).toBoolean else true
      val truncLength = if (args.length > 13) args(13).toInt else 6000
      val useBalancePartition = if (args.length > 14) args(14).toBoolean else false
      val srcIndex = if (args.length > 15) args(15).toInt else 0
      val dstIndex = if (args.length > 16) args(16).toInt else 1
      val weightIndex = if (args.length > 17) args(17).toInt else 2
      val setCheckPoint = if (args.length > 18) args(18).toBoolean else false
      val epochNum = if (args.length > 19) args(19).toInt else 1
      val percent = if (args.length > 20) args(20).toFloat else 0.7f

      // PS setup
      val start = System.currentTimeMillis()

      val data = GraphIO.load(input, isWeighted = isWeighted, srcIndex, dstIndex, weightIndex, sep = delimiter)
      data.printSchema()
      println("the data loading time: " + ((System.currentTimeMillis()-start)/1000.0))

      val n2v = new Node2Vec()
        .setPSPartitionNum(psPartNum)
        .setPartitionNum(dataPartNum)
        .setBatchSize(batchSize)
        .setEpochNum(epochNum)
        .setWalkLength(walkLength)
        .setPValue(pValue)
        .setQValue(qValue)
        .setNeedReplicaEdge(needReplicaEdge)
        .setIsTrunc(useTrunc)
        .setIsWeighted(isWeighted)
        .setTruncLength(truncLength)
        .setUseBalancePartition(useBalancePartition)
        .setStorageLevel(storageLevel)
        .setCheckPoint(setCheckPoint)
        .setBalancePartitionPercent(percent)

      n2v.setOutputDir(output)
      // sampling walkpaths
      println("begin to fit|train ...")
      if (!isWeighted) {
        n2v.transform(data)
      } else {
        n2v.transformWithWeights(data)
      }
      println("fit|train finished!")
      val end = System.currentTimeMillis()
      println(s"the elapsed time: ${1.0 * (end - start) / 1000}")

      println("Stop PS ...")
      Node2Vec.stopPS()
      println("PS Stopped!")
    }

    println("Stop Spark ...")
    spark.stop()
    println("Spark Stopped!")
  }


  def timeLoop[T](items: Seq[T], labelFn: T => String = (_: T).toString)(block: T => Unit): Unit = {
    val totalStart = System.nanoTime()
    val totalItems = items.length

    for ((item, idx) <- items.zipWithIndex) {
      val iterStart = System.nanoTime()
      println(s"Starting ${idx + 1}/$totalItems: ${labelFn(item)}")

      block(item)

      val iterEnd = System.nanoTime()
      val iterSecs = (iterEnd - iterStart) / 1e9
      println(f"Finished in $iterSecs%.2f seconds")

      val elapsed = (System.nanoTime() - totalStart) / 1e9
      val estTotal = elapsed / (idx + 1) * totalItems
      val estRemaining = estTotal - elapsed
      println(f"Estimated remaining time: $estRemaining%.2f seconds\n")
    }

    val totalEnd = System.nanoTime()
    println(f"Total time: ${(totalEnd - totalStart) / 1e9}%.2f seconds")
  }


}
