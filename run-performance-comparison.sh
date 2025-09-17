#!/bin/bash

echo "=========================================="
echo "AWS GLUE SCHEMA REGISTRY PERFORMANCE COMPARISON"
echo "Kafka Serializer vs Facade Serializer"
echo "=========================================="

echo ""
echo "🚀 Running Kafka Serializer Performance Tests..."
echo "=================================================="
mvn test -Dtest=GSRKafkaSerializerPerformanceTest -pl integration-tests -Dorg.slf4j.simpleLogger.defaultLogLevel=ERROR

echo ""
echo ""
echo "🏗️  Running Facade Serializer Performance Tests..."
echo "=================================================="
mvn test -Dtest=GSRPerformanceTest#testJSONSerializationPerformance,GSRPerformanceTest#testAvroSerializationPerformance,GSRPerformanceTest#testProtobufSerializationPerformance -pl integration-tests -Dorg.slf4j.simpleLogger.defaultLogLevel=ERROR

echo ""
echo "✅ Performance comparison completed!"
echo "=========================================="