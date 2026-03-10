1)download it from here :https://www.enterprisedb.com/downloads/postgres-postgresql-downloads

2)remember the password 

3)create a database ( i named it network_analyser)

4)run this in your terminal  to connect FastAPI to PostgreSQL:uv add sqlalchemy psycopg2-binary

5)after running the server , go to pgAdmin(your PostgreSQL) , go to network_analyser(the name of the database you created), then Schemas, Public , Tables and you will see the 2 tables that we created

6)in devices , in the query , write ' SELECT * FROM devices ' and run it (every time you run it , the table will refresh and the updated table will appear)

