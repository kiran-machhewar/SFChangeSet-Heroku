call git init
call heroku git:remote -a kmforce4
call git add .
call git commit -am "make it better"
call git push heroku master 
call heroku ps:scale web=1