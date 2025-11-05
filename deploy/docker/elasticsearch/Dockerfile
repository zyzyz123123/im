FROM elasticsearch:7.17.16

# 安装 IK 中文分词器（使用国内镜像，--batch 自动确认权限）
RUN /usr/share/elasticsearch/bin/elasticsearch-plugin install --batch \
    https://get.infini.cloud/elasticsearch/analysis-ik/7.17.16

