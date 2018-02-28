package com.gu.inspectorlambda.aws

import com.amazonaws.services.inspector.AmazonInspector
import com.amazonaws.services.inspector.model._
import com.gu.inspectorlambda.chiefinspector.ChiefInspector.inspectionTagName
import com.typesafe.scalalogging.StrictLogging
import scala.collection.JavaConverters._

class AWSInspector(val client: AmazonInspector) extends StrictLogging {

  def getResourceGroup(name: String): Option[String] = {

    val assessmentTargetArns = client.listAssessmentTargets(new ListAssessmentTargetsRequest()).getAssessmentTargetArns

    if (assessmentTargetArns.isEmpty)
      None
    else
      client.describeAssessmentTargets(new DescribeAssessmentTargetsRequest().withAssessmentTargetArns(assessmentTargetArns)).getAssessmentTargets.asScala
        .flatMap(assessmentTarget => {
          val describeResourceGroupsRequest = new DescribeResourceGroupsRequest().withResourceGroupArns(assessmentTarget.getResourceGroupArn)
          client.describeResourceGroups(describeResourceGroupsRequest).getResourceGroups.asScala
            .filter(resourceGroup => {
              resourceGroup.getTags.asScala
                .exists(p => p.getKey.equals(inspectionTagName) && p.getValue.equals(name))
            })
            .map(resourceGroup => resourceGroup.getArn)
        })
        .headOption
  }

  def createResourceGroup(name: String): String = {
    val createResourceGroupRequest = new CreateResourceGroupRequest()
      .withResourceGroupTags(new ResourceGroupTag()
        .withKey(inspectionTagName)
        .withValue(name)
      )
    val createResourceGroupResult = client.createResourceGroup(createResourceGroupRequest)
    createResourceGroupResult.getResourceGroupArn
  }

  def getAllAssessmentTargets(token: Option[String]): List[AssessmentTarget] = {

    val assessmentTargetsRequest = token match {
      case Some(s) => new ListAssessmentTargetsRequest().withNextToken(s)
      case None => new ListAssessmentTargetsRequest()
    }

    val assessmentTargets = client.listAssessmentTargets(assessmentTargetsRequest)
    val arns = assessmentTargets.getAssessmentTargetArns
    if (arns.isEmpty)
      List()
    else {
      val assessmentTargetDescriptions = client.describeAssessmentTargets(new DescribeAssessmentTargetsRequest().withAssessmentTargetArns(arns)).getAssessmentTargets.asScala.toList

      assessmentTargets.getNextToken match {
        case s: String => assessmentTargetDescriptions ::: getAllAssessmentTargets(Some(s))
        case _ => assessmentTargetDescriptions
      }
    }
  }

  def getAssessmentTarget(name: String, arn: String): Option[String] = {
//    logger.info("here")
//    logger.info(name)
    val assessmentTargets = getAllAssessmentTargets(None)
//    logger.info(assessmentTargets.toString)
//    logger.info(arn)

    val matchingAssessmentTargets = assessmentTargets.filter(assessmentTarget => assessmentTarget.getName.equals(name))

//    logger.info(matchingAssessmentTargets.size.toString)
//    matchingAssessmentTargets.foreach(mat => {
//      logger.info(mat.getArn)
//      logger.info(mat.getResourceGroupArn)
//    })
    // delete if arn is not correct
    matchingAssessmentTargets
      .filter(assessmentTarget => !assessmentTarget.getResourceGroupArn.equals(arn))
      .foreach(assessmentTarget => {
//        logger.info(s"Deleting ${assessmentTarget.getArn}")
        val deleteAssessmentTargetRequest = new DeleteAssessmentTargetRequest()
          .withAssessmentTargetArn(assessmentTarget.getArn)
        client.deleteAssessmentTarget(deleteAssessmentTargetRequest)
      })

    // Return if arn is correct
    matchingAssessmentTargets
      .filter(assessmentTarget => {
//        logger.info(s"Deleting ${assessmentTarget.getArn}");
        assessmentTarget.getResourceGroupArn.equals(arn)
      })
      .map(assessmentTarget => assessmentTarget.getArn)
      .headOption
  }

  def createAssessmentTarget(name: String, arn: String): String = {

    val createAssessmentTargetRequest = new CreateAssessmentTargetRequest()
      .withResourceGroupArn(arn)
      .withAssessmentTargetName(name)
    val createAssessmentTargetResult = client.createAssessmentTarget(createAssessmentTargetRequest)
    createAssessmentTargetResult.getAssessmentTargetArn
  }

  def getAssessmentTemplate(name: String, arn: String): Option[String] = {

    val assessmentTemplateArns = client.listAssessmentTemplates(new ListAssessmentTemplatesRequest()).getAssessmentTemplateArns
    if (assessmentTemplateArns.isEmpty)
      None
    else {

      val matchingAssessmentTemplates = client.describeAssessmentTemplates(new DescribeAssessmentTemplatesRequest().withAssessmentTemplateArns(assessmentTemplateArns)).getAssessmentTemplates.asScala
        .filter(assessmentTemplate => assessmentTemplate.getName.equals(name))

      // delete if arn is not correct
      matchingAssessmentTemplates
        .filter(assessmentTemplate => !assessmentTemplate.getAssessmentTargetArn.equals(arn))
        .foreach(assessmentTemplate => {
          val deleteAssessmentTemplateRequest = new DeleteAssessmentTemplateRequest()
            .withAssessmentTemplateArn(assessmentTemplate.getArn)
          client.deleteAssessmentTemplate(deleteAssessmentTemplateRequest)
        })

      // Return if arn is correct
      matchingAssessmentTemplates
        .filter(assessmentTemplate => assessmentTemplate.getAssessmentTargetArn.equals(arn))
        .map(assessmentTemplate => assessmentTemplate.getArn)
        .headOption
    }
  }

  def createAssessmentTemplate(name: String, arn: String): String = {
    val createAssessmentTemplateRequest = new CreateAssessmentTemplateRequest()
      .withAssessmentTargetArn(arn)
      .withDurationInSeconds(3600)
      .withRulesPackageArns(
        "arn:aws:inspector:eu-west-1:357557129151:rulespackage/0-lLmwe1zd",
        "arn:aws:inspector:eu-west-1:357557129151:rulespackage/0-SnojL3Z6"
      )
      .withUserAttributesForFindings(new Attribute().withKey(inspectionTagName).withValue(name))
      .withAssessmentTemplateName(name)
    val createAssessmentTemplateResult = client.createAssessmentTemplate(createAssessmentTemplateRequest)
    createAssessmentTemplateResult.getAssessmentTemplateArn
  }

  def startAssessmentRun(name: String, assessmentTemplateArn: String): Unit = {
    val startAssessmentRunRequest = new StartAssessmentRunRequest()
      .withAssessmentRunName(name)
      .withAssessmentTemplateArn(assessmentTemplateArn)
    try {
      client.startAssessmentRun(startAssessmentRunRequest)
      logger.error(s"Assessment run started for $name")
    } catch {
      case e: InvalidInputException =>
        logger.error(s"Unable to start Assessment run '$name' (${e.getMessage})")
    }
  }

}