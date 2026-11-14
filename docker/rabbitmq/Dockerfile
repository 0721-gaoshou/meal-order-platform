# M5：官方镜像不带延迟交换机插件，定制构建补齐
# 插件来源 https://github.com/rabbitmq/rabbitmq-delayed-message-exchange/releases/tag/v3.13.0
# .ez 随仓库 vendored（45KB），保证离线可复现构建；版本须与服务器 3.13.x 匹配
FROM rabbitmq:3.13-management

COPY rabbitmq_delayed_message_exchange-3.13.0.ez /opt/rabbitmq/plugins/
RUN rabbitmq-plugins enable --offline rabbitmq_delayed_message_exchange