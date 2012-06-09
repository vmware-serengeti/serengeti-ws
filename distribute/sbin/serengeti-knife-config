#!/usr/bin/expect

cd /opt/serengeti
spawn /usr/bin/knife configure -i -y

expect "Please enter the chef server URL"
send "\n"

expect "Please enter a clientname for the new client:"
send "serengeti\n"

expect "Please enter the existing admin clientname:"
send "\n"

expect "Please enter the location of the existing admin client's private key:"
send "\.chef\/webui.pem\n"

expect "Please enter the validation clientname:"
send "\n"

expect "Please enter the location of the validation key:"
send "\.chef\/validation.pem\n"

expect "Please enter the path to a chef repository (or leave blank):"
send "\n"

expect eof
