import bcrypt
# insert the password of your choosing in between quotations
password = b"1234"
hashed = bcrypt.hashpw(password, bcrypt.gensalt())

print(hashed.decode())
# paste the output in the env variable 
