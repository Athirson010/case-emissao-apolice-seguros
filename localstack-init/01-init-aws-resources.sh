#!/bin/bash

echo "========================================="
echo "Initializing LocalStack AWS Resources"
echo "========================================="

# Set AWS credentials for LocalStack
export AWS_ACCESS_KEY_ID=test
export AWS_SECRET_ACCESS_KEY=test
export AWS_DEFAULT_REGION=us-east-1

# Wait for LocalStack to be ready
sleep 5

echo ""
echo "1. Creating SNS Topic..."
TOPIC_ARN=$(awslocal sns create-topic \
    --name insurance-topic \
    --region us-east-1 \
    --output text --query 'TopicArn')

echo "   ✓ SNS topic created: $TOPIC_ARN"

echo ""
echo "2. Creating SQS Queues..."

# Create order-service-consumer-insurance-response queue
QUEUE1_URL=$(awslocal sqs create-queue \
    --queue-name order-service-consumer-insurance-response \
    --region us-east-1 \
    --output text --query 'QueueUrl')
echo "   ✓ SQS Queue created: order-service-consumer-insurance-response"

# Create order-service-consumer-payment-response queue
QUEUE2_URL=$(awslocal sqs create-queue \
    --queue-name order-service-consumer-payment-response \
    --region us-east-1 \
    --output text --query 'QueueUrl')
echo "   ✓ SQS Queue created: order-service-consumer-payment-response"

# Create order-service-fraud-consumer queue
QUEUE3_URL=$(awslocal sqs create-queue \
    --queue-name order-service-fraud-consumer \
    --region us-east-1 \
    --output text --query 'QueueUrl')
echo "   ✓ SQS Queue created: order-service-fraud-consumer"

echo ""
echo "3. Subscribing SQS Queues to SNS Topic..."

# Get Queue ARNs
QUEUE1_ARN=$(awslocal sqs get-queue-attributes \
    --queue-url $QUEUE1_URL \
    --attribute-names QueueArn \
    --region us-east-1 \
    --output text --query 'Attributes.QueueArn')

QUEUE2_ARN=$(awslocal sqs get-queue-attributes \
    --queue-url $QUEUE2_URL \
    --attribute-names QueueArn \
    --region us-east-1 \
    --output text --query 'Attributes.QueueArn')

QUEUE3_ARN=$(awslocal sqs get-queue-attributes \
    --queue-url $QUEUE3_URL \
    --attribute-names QueueArn \
    --region us-east-1 \
    --output text --query 'Attributes.QueueArn')

# Subscribe queues to SNS topic
awslocal sns subscribe \
    --topic-arn $TOPIC_ARN \
    --protocol sqs \
    --notification-endpoint $QUEUE1_ARN \
    --region us-east-1

awslocal sns subscribe \
    --topic-arn $TOPIC_ARN \
    --protocol sqs \
    --notification-endpoint $QUEUE2_ARN \
    --region us-east-1

awslocal sns subscribe \
    --topic-arn $TOPIC_ARN \
    --protocol sqs \
    --notification-endpoint $QUEUE3_ARN \
    --region us-east-1

echo "   ✓ All queues subscribed to SNS topic"

echo ""
echo "========================================="
echo "LocalStack Resources Summary"
echo "========================================="
echo ""
echo "SNS Topics:"
awslocal sns list-topics --region us-east-1

echo ""
echo "SQS Queues:"
awslocal sqs list-queues --region us-east-1

echo ""
echo "========================================="
echo "✓ LocalStack initialization completed!"
echo "========================================="
