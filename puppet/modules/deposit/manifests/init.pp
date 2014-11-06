class deposit(
  $enable_debug,
  $fedora_admin_username,
  $fedora_admin_password,
  $fedora_forwarded_username,
  $fedora_forwarded_password,
  $jms_host,
  $jms_port,
  $mulgara_protocol,
  $mulgara_host,
  $mulgara_port,
  $mulgara_model_uri,
  $workers_num,
  $workers_mets_num,
) {
  
  package { "redis":
    ensure => installed,
  }
  
  service { "redis":
    ensure => true,
    enable => true,
    hasrestart => true,
    hasstatus => true,
    require => Package["redis"],
  }
  
  package { "cdr-commons-daemon":
    ensure => "1.0.15-2",
    require => [
      Package["java-1.7.0-openjdk-devel"],
      Yumrepo["cdr-server"]
    ],
  }
  
  file { "/opt/deposit":
    source => "puppet:///modules/deposit/home",
    recurse => remote,
    purge => false,
    owner => "tomcat",
    group => "tomcat",
    require => User["tomcat"],
  }
  
  file { "/opt/deposit/deposit.jar":
    ensure => "link",
    target => "/opt/deploy/deposit/deposit.jar",
  }
  
  file { "/opt/deposit/deposit.properties":
    content => template("deposit/home/deposit.properties.erb"),
    owner => "tomcat",
    group => "tomcat",
    require => User["tomcat"],
  }
  
  file { "/etc/init.d/deposit":
    content => template("deposit/deposit.sh.erb"),
    mode => "a+x",
    require => User["tomcat"],
  }
  
  exec { "/sbin/chkconfig --add deposit":
    unless => "/sbin/chkconfig deposit",
    require => File["/etc/init.d/deposit"],
  }
  
}
