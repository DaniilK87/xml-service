# Интерактивное меню: 
- docker exec -it xml-app java -jar app.jar
# Очистка БД: 
- docker exec -it xml-postgres psql -U postgres -d xmldb -c "DROP TABLE IF EXISTS offers, categories, currency CASCADE;"
