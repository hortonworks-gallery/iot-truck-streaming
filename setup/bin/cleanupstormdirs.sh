# paths on an Ambari install will include /mnt
if [ -d "/mnt/hadoop/storm" ]; then
	rm -rf /mnt/hadoop/storm/supervisor/isupervisor/*
	rm -rf /mnt/hadoop/storm/supervisor/localstate/*
	rm -rf /mnt/hadoop/storm/supervisor/stormdist/*
	rm -rf /mnt/hadoop/storm/supervisor/tmp/*
	rm -rf /mnt/hadoop/storm/workers/*
	rm -rf /mnt/hadoop/storm/workers-users/*
fi

# paths in sandbox start with /hadoop
if [ -d "/hadoop/storm" ]; then
	rm -rf /hadoop/storm/supervisor/isupervisor/*
	rm -rf /hadoop/storm/supervisor/localstate/*
	rm -rf /hadoop/storm/supervisor/stormdist/*
	rm -rf /hadoop/storm/supervisor/tmp/*
	rm -rf /hadoop/storm/workers/*
	rm -rf /hadoop/storm/workers-users/*
	rm -rf /hadoop/storm/nimbus/stormdist/*
fi

# this path is the same in sandbox or Ambari installs
if [ -d "/var/log/storm" ]; then
	rm -rf /var/log/storm/truck*
fi
