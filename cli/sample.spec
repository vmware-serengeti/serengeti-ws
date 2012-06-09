[
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
      ]
    },
    {
      "name": "client",
      "roles": [
        "hadoop_client",
        "hive",
        "pig"
      ],
      "cpuNum": 1,
      "storage": {
        "type": "LOCAL",
        "sizeGB": 10
      },        
      "rpNames": [
        "rp3"
      ]
    }
]  
