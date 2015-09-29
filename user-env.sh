
# User configured section

# Ambari configuration
host='sandbox.hortonworks.com:8080'
cluster='Sandbox'
user='admin'
pass='admin'

# End user configured section


# Sandbox hostname override - if the user doesn't edit this file, and runs on sandbox, we can make it work with known values
if [ `hostname` = sandbox.hortonworks.com ]; then
        cluster='Sandbox'
		host='sandbox.hortonworks.com:8080'
fi

#TODO: Actually make these environment variables? Or, add this to config.properties instead.
