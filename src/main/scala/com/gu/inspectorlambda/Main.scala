package com.gu.inspectorlambda

import com.amazonaws.auth.profile.ProfileCredentialsProvider
import com.amazonaws.regions.Regions
import com.amazonaws.services.ec2.AmazonEC2AsyncClientBuilder
import com.amazonaws.services.inspector.AmazonInspectorClientBuilder
import com.gu.inspectorlambda.chiefinspector.ChiefInspector
import com.gu.inspectorlambda.model.TagCombo

object Main {
  def main(args: Array[String]): Unit = {
    if (args.length < 1) {
      println("Must provide a credentials stanza name")
      System.exit(1)
    }
    val ec2Client =  AmazonEC2AsyncClientBuilder.standard()
      .withCredentials(new ProfileCredentialsProvider(args(0)))
      .withRegion(Regions.EU_WEST_1)
      .build()
    val inspectorClient =  AmazonInspectorClientBuilder.standard()
      .withCredentials(new ProfileCredentialsProvider(args(0)))
      .withRegion(Regions.EU_WEST_1)
      .build()
    val argOpts = args.lift
    val filter: Option[TagCombo => Boolean] = for {
      app   <- argOpts(1)
      stack <- argOpts(2)
      stage <- argOpts(3)
    } yield ({
      case TagCombo(Some(`app`), Some(`stack`), Some(`stage`)) => true
      case _ => false
    })

    ChiefInspector.createAndRunAssessments(ec2Client, inspectorClient, filter)
  }
}
