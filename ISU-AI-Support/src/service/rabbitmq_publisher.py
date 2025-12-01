"""
RabbitMQ Publisher Service
Publishes notification events to RabbitMQ for Notification Microservice to consume
"""

import pika
import json
import os
import logging
from typing import Optional
from dto.NotificationEvent import NotificationEvent

logger = logging.getLogger(__name__)


class RabbitMQPublisher:
    """
    RabbitMQ Publisher for sending notification events
    Publishes to the same exchange/routing-key that Notification Service listens to
    """

    def __init__(self):
        """Initialize RabbitMQ connection parameters from environment variables"""
        self.host = os.getenv("RABBITMQ_HOST", "localhost")
        self.port = int(os.getenv("RABBITMQ_PORT", "5672"))
        self.username = os.getenv("RABBITMQ_USERNAME", "guest")
        self.password = os.getenv("RABBITMQ_PASSWORD", "guest")
        self.virtual_host = os.getenv("RABBITMQ_VIRTUAL_HOST", "/")

        # Exchange and routing key - must match Notification Service configuration
        self.exchange_name = os.getenv("RABBITMQ_EXCHANGE", "notification.exchange")
        self.routing_key = os.getenv("RABBITMQ_ROUTING_KEY", "notification.send")
        self.queue_name = os.getenv("RABBITMQ_QUEUE", "notification.queue")

        self.connection = None
        self.channel = None

        logger.info(f"RabbitMQ Publisher initialized with host={self.host}, port={self.port}, exchange={self.exchange_name}")

    def _connect(self):
        """Establish connection to RabbitMQ and declare exchange/queue"""
        try:
            if self.connection is None or self.connection.is_closed:
                credentials = pika.PlainCredentials(self.username, self.password)
                parameters = pika.ConnectionParameters(
                    host=self.host,
                    port=self.port,
                    virtual_host=self.virtual_host,
                    credentials=credentials,
                    heartbeat=600,
                    blocked_connection_timeout=300
                )

                self.connection = pika.BlockingConnection(parameters)
                self.channel = self.connection.channel()

                # Declare exchange (idempotent - will not recreate if exists)
                self.channel.exchange_declare(
                    exchange=self.exchange_name,
                    exchange_type='topic',
                    durable=True
                )

                # Declare queue (idempotent)
                self.channel.queue_declare(
                    queue=self.queue_name,
                    durable=True,
                    arguments={
                        'x-dead-letter-exchange': 'notification.dlx'
                    }
                )

                # Bind queue to exchange with routing key
                self.channel.queue_bind(
                    queue=self.queue_name,
                    exchange=self.exchange_name,
                    routing_key=self.routing_key
                )

                logger.info(f"Successfully connected to RabbitMQ and declared queue: {self.queue_name}")

        except Exception as e:
            logger.error(f"Failed to connect to RabbitMQ: {str(e)}")
            raise

    def publish_notification_event(self, event: NotificationEvent) -> bool:
        """
        Publish a notification event to RabbitMQ

        Args:
            event: NotificationEvent to publish

        Returns:
            bool: True if published successfully, False otherwise
        """
        try:
            self._connect()

            # Convert Pydantic model to dict with camelCase keys (matching Java)
            message_body = event.model_dump(by_alias=True, exclude_none=True)

            # Publish message
            self.channel.basic_publish(
                exchange=self.exchange_name,
                routing_key=self.routing_key,
                body=json.dumps(message_body),
                properties=pika.BasicProperties(
                    delivery_mode=2,  # Make message persistent
                    content_type='application/json'
                )
            )

            logger.info(f"Published notification event for recipient: {event.recipient_id}, type: {event.target_type}")
            return True

        except Exception as e:
            logger.error(f"Failed to publish notification event: {str(e)}")
            return False

    def close(self):
        """Close RabbitMQ connection"""
        try:
            if self.connection and not self.connection.is_closed:
                self.connection.close()
                logger.info("RabbitMQ connection closed")
        except Exception as e:
            logger.error(f"Error closing RabbitMQ connection: {str(e)}")

    def __del__(self):
        """Cleanup on object destruction"""
        self.close()


# Singleton instance
_rabbitmq_publisher = None


def get_rabbitmq_publisher() -> RabbitMQPublisher:
    """
    Get singleton instance of RabbitMQ Publisher

    Returns:
        RabbitMQPublisher: Singleton instance
    """
    global _rabbitmq_publisher
    if _rabbitmq_publisher is None:
        _rabbitmq_publisher = RabbitMQPublisher()
    return _rabbitmq_publisher
