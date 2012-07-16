{
  "nodeGroups":[
    {
      "name": "master",
      "roles": [
        "hadoop_namenode",
        "hadoop_jobtracker"
      ],
      "instanceNum": 1,
      "instanceType": "LARGE",
      "cpuNum": 6,
      "memCapacityMB": 2048,
      "haFlag": false
    },
    {
      "name": "worker",
      "roles": [
        "hadoop_tasktracker",
        "hadoop_datanode"
      ],
      "instanceNum": 4,
      "cpuNum": 2,
      "memCapacityMB": 1024,	
      "storage": {
        "type": "LOCAL",
        "sizeGB": 10
      },        
      "rpNames": [
        "rp1",
        "rp2" 
      ],
       "configuration": {
	     "hadoop": {
	        "core-site.xml" : {
	            "hadoop.tmp.dir": "/temp"
	        },
	        "hdfs-site.xml" : {
	           "dfs.namenode.logging.level": 2
	        },
	       "mapred-site.xml" : {
	           "mapred.map.tasks": 3
	       },
	      "hadoop-env.sh" : {
	           "JAVA_HOME": "/path/to/javahome"
	       },
	      "log4j.properties" : {
	            "hadoop.root.logger": "DEBUG,console"
	      }
	    }
	  }
    },
    {
      "name": "client",
      "roles": [
        "hadoop_client",
        "hive",
        "pig"
      ],
      "instanceNum": 1,
      "cpuNum": 1,
      "storage": {
        "type": "LOCAL",
        "sizeGB": 10
      },        
      "rpNames": [
        "rp3"
      ]
    }
  ],
  "configuration": {
    "hadoop": {
        "core-site.xml" : {
            "hadoop.tmp.dir": "/temp"
        },
       "hdfs-site.xml" : {
           "dfs.namenode.logging.level": 2
      },
      "mapred-site.xml" : {
           "mapred.map.tasks": 3
      },
     "hadoop-env.sh" : {
           "JAVA_HOME": "/path/to/javahome"
      },
      "log4j.properties" : {
            "hadoop.root.logger": "DEBUG,console"
     }
   }
 }
}  
