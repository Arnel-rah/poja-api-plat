package api.poja.io.repository.model;

import static jakarta.persistence.FetchType.EAGER;
import static jakarta.persistence.GenerationType.IDENTITY;
import static java.nio.charset.StandardCharsets.UTF_8;

import api.poja.io.endpoint.rest.model.EventQueue;
import api.poja.io.endpoint.rest.model.EventQueueResourceGroup;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import java.net.URI;
import java.net.URLEncoder;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Table(name = "\"event_stack_resource\"")
@EqualsAndHashCode
@ToString
public class EventStackResource {
  private static final String WEBSITE_QUEUE_URI_TEMPLATE =
      "https://%s.console.aws.amazon.com/sqs/v3/home#/queues/%s";
  private static final String SQS_QUEUE_URL_TEMPLATE = "https://sqs.%s.amazonaws.com/%s/%s";

  @Id
  @GeneratedValue(strategy = IDENTITY)
  private String id;

  @ElementCollection(fetch = EAGER)
  @CollectionTable(
      name = "event_stack_resource_dead_letter_queue_names",
      joinColumns = @JoinColumn(name = "event_stack_resource_id"))
  @OrderColumn(name = "queue_index")
  @Column(name = "dead_letter_queue_name")
  private List<String> deadLetterQueueNames;

  @ElementCollection(fetch = EAGER)
  @CollectionTable(
      name = "event_stack_resource_mailbox_queue_names",
      joinColumns = @JoinColumn(name = "event_stack_resource_id"))
  @OrderColumn(name = "queue_index")
  @Column(name = "mailbox_queue_name")
  private List<String> mailboxQueueNames;

  private String eventStackPolicyDocumentName;
  private String stackId;
  private String envId;
  private String appEnvDeplId;
  private Instant creationTimestamp;

  public static URI getConsoleQueueUri(String region, String accountId, String queueName) {
    var sqsQueueUri = getSqsQueueUri(region, accountId, queueName);
    return URI.create(
        WEBSITE_QUEUE_URI_TEMPLATE.formatted(
            region, URLEncoder.encode(String.valueOf(sqsQueueUri), UTF_8)));
  }

  public static URI getSqsQueueUri(String region, String accountId, String queueName) {
    return URI.create(SQS_QUEUE_URL_TEMPLATE.formatted(region, accountId, queueName));
  }

  public static String computeQueueArn(String region, String accountId, String queueName) {
    return "arn:aws:sqs:%s:%s:%s".formatted(region, accountId, queueName);
  }

  private static EventQueue getEventQueue(String region, String accountId, String queueName) {
    if (queueName == null) {
      return null;
    }
    return new EventQueue()
        .name(queueName)
        .resourceUri(getConsoleQueueUri(region, accountId, queueName));
  }

  public List<EventQueueResourceGroup> queueResourceGroups(String region, String accountId) {
    if (mailboxQueueNames == null || mailboxQueueNames.isEmpty()) {
      return List.of();
    }
    List<EventQueueResourceGroup> groups = new ArrayList<>();
    for (int i = 0; i < mailboxQueueNames.size(); i++) {
      EventQueue queue = getEventQueue(region, accountId, mailboxQueueNames.get(i));
      EventQueue dlQueue = getEventQueue(region, accountId, deadLetterQueueNames.get(i));
      if (queue != null || dlQueue != null) {
        groups.add(new EventQueueResourceGroup().queue(queue).dlQueue(dlQueue));
      }
    }
    return groups;
  }
}
