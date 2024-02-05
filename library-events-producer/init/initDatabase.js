try {
db.createUser(
	{
	   user:"lbr-evnt-listener",
	   pwd:"qwerty",
	   roles:[
	      {
	         role:"readWrite",
	         db:"library-events"
	      }
	  ]
	}
)

print("User Created");

} catch(error) {
    print("Failed to create user: \n ${error}")
}