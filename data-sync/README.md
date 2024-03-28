# data-sync

#TODO : Les commandes suivantes sont à corriger et classer dans une doc ailleurs

k exec -n provoly-dev -c kafka kafka-0 -- <cmd>

docker compose exec -it kafka kafka-consumer-groups.sh --bootstrap-server localhost:9092 --list
docker compose exec -it kafka kafka-consumer-groups.sh --bootstrap-server localhost:9092 --describe --group data-link
docker compose exec -it kafka kafka-consumer-groups.sh --bootstrap-server localhost:9092 --delete --group data-link
docker compose exec -it kafka kafka-consumer-groups.sh --bootstrap-server localhost:9092 --reset-offsets --to-earliest --topic relation-aggregate --group data-sync --execute


docker compose exec kafka kafka-console-consumer.sh --bootstrap-server localhost:9092 --from-beginning --timeout-ms 2000 \
--property print.offset=true --property print.partition=true --property print.key=true --property print.value=true \
--topic class-f13c79a6-a1f3-4e4b-9234-6de5f08617b8 | grep 202000000001

docker compose exec kafka kafka-console-consumer.sh --bootstrap-server localhost:9092 --from-beginning --timeout-ms 2000 \
--property print.offset=true --property print.partition=true --property print.key=true --property print.value=true \
--topic class-52919ac0-0dea-48fc-b200-09a016e755c0 | grep 202000000001

docker compose exec kafka kafka-console-consumer.sh --bootstrap-server localhost:9092 --from-beginning --timeout-ms 2000 \
--property print.offset=true --property print.partition=true --property print.key=true --property print.value=true \
--topic data-link-caracteristique-by-num-acc-repartition | grep 202000000001

docker compose exec kafka kafka-console-consumer.sh --bootstrap-server localhost:9092 --from-beginning --timeout-ms 2000 \
--property print.offset=true --property print.partition=true --property print.key=true --property print.value=true \
--topic relation-aggregate | grep 202000000001