package api.poja.io.aws.sqs;

import static software.amazon.awssdk.services.sqs.model.QueueAttributeName.POLICY;

import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.policybuilder.iam.IamPolicy;
import software.amazon.awssdk.policybuilder.iam.IamPolicyReader;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.QueueAttributeName;

@Component
public class SqsComponent {
  private final SqsClient sqsClient;

  public SqsComponent(@Qualifier("targetAccountSqsClient") SqsClient sqsClient) {
    this.sqsClient = sqsClient;
  }

  public void updateSqsQueuePolicy(String queueUrl, IamPolicy newPolicy) {
    var policy = sqsClient.getQueueAttributes(req -> req.queueUrl(queueUrl).attributeNames(POLICY));
    Map<QueueAttributeName, String> currentAttributes = policy.attributes();
    var crupdatedPolicy = crupdatePolicy(currentAttributes, newPolicy);
    var copyOfCurrentAttributes = new HashMap<>(currentAttributes);
    copyOfCurrentAttributes.put(POLICY, crupdatedPolicy.toJson());
    sqsClient.setQueueAttributes(req -> req.queueUrl(queueUrl).attributes(copyOfCurrentAttributes));
  }

  private IamPolicy crupdatePolicy(
      Map<QueueAttributeName, String> attributes, IamPolicy newPolicy) {
    if (!attributes.containsKey(POLICY)) {
      return newPolicy;
    }
    var currentPolicy = readAsUserPolicy(attributes.get(POLICY));
    var currentPolicyStatements = currentPolicy.statements();
    var currentPolicyBuilder = currentPolicy.toBuilder();
    newPolicy
        .statements()
        .forEach(
            st -> {
              if (!currentPolicyStatements.contains(st)) {
                currentPolicyBuilder.addStatement(st);
              }
            });
    return currentPolicyBuilder.build();
  }

  private static IamPolicy readAsUserPolicy(String policyAsString) {
    return IamPolicyReader.create().read(policyAsString);
  }
}
