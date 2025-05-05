package com.tencent.angel.spark.examples.local

import com.tencent.angel.graph.embedding.node2vec.Node2Vec
import com.tencent.angel.graph.utils.GraphIO
import com.tencent.angel.spark.examples.local.dir.getFile
import com.tencent.angel.spark.ml.core.ArgsUtil
import org.apache.spark.sql.SparkSession
import org.apache.spark.storage.StorageLevel

import java.io.File


object Node2VecExample {

  def main(args: Array[String]): Unit = {
      // Use Struct2GO edge list directory
      println("hahahaha")
      val path = new File("../../../data/proteins_edgs")
      for(name <- getFile(path)) {
          val names1 = name.toString.split("/")
          val params = ArgsUtil.parse(args)
          val input = params.getOrElse("input", name.toString)
          // Output to node2vec directory, preserving filename
          val output = params.getOrElse("output", "data/node2vec/" + names1.last.replace(".txt", ""))
          val batchSize = params.getOrElse("batchSize", "128").toInt
          val psPartNum = params.getOrElse("psPartNum", "2").toInt
          val dataPartNum = params.getOrElse("dataPartNum", "1").toInt
          val storageLevel = StorageLevel.fromString(params.getOrElse("storageLevel", "MEMORY_ONLY"))
          val delimiter = params.getOrElse("delimiter", "space") match {
              case "space" => " "
              case "comma" => ","
              case "tab" => "\t"
          }
          val mode = "local"
          val isWeighted = params.getOrElse("isWeighted", "false").toBoolean
          val walkLength = params.getOrElse("walkLength", "30").toInt
          val pValue = params.getOrElse("pValue", "0.8").toDouble
          val qValue = params.getOrElse("qValue", "1.2").toDouble
          val needReplicaEdge = params.getOrElse("needReplicaEdge", "false").toBoolean
          val useTrunc = params.getOrElse("useTrunc", "true").toBoolean
          val truncLength = params.getOrElse("truncLength", "6000").toInt
          val useBalancePartition = params.getOrElse("useBalancePartition", "false").toBoolean
          val srcIndex = params.getOrElse("srcIndex", "0").toInt
          val dstIndex = params.getOrElse("dstIndex", "1").toInt
          val weightIndex = params.getOrElse("weightIndex", "2").toInt
          val setCheckPoint = params.getOrElse("setCheckPoint", "false").toBoolean
          val epochNum = params.getOrElse("epochNum", "1").toInt
          val percent = params.getOrElse("balancePartitionPercent", "0.7").toFloat

          // Spark setup
          val spark = SparkSession.builder()
            .master(mode)
            .appName("node2vec")
            .getOrCreate()

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

          println("Stop Spark ...")
          spark.stop()
          println("Spark Stopped!")
      }

  }



}
