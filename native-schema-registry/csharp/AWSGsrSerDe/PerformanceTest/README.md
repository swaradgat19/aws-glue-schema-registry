# AWS Glue Schema Registry C# Performance Test

This is a simple performance test application for the AWS Glue Schema Registry C# library.

## What it tests

- **Serializer creation time**: How long it takes to initialize the serializer
- **First serialization**: Time including schema registration with AWS Glue
- **Cached serialization**: Time for subsequent serializations using cached schema
- **Average serialization**: Performance over multiple iterations
- **Round-trip test**: Full serialize + deserialize cycle

## Prerequisites

