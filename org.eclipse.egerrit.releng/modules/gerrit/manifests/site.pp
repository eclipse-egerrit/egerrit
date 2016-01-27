define gerrit::site (
  $envid        = "$title",
  $port,
  $sshport,
  $version,
  $allbasicauth = false,
  $certauth     = false,
  $digestauth   = false,
  $authtype     = "DEVELOPMENT_BECOME_ANY_ACCOUNT",
  $envtype      = "gerrit",
  $envinfo      = "",
  $envdefault = false,
  $base         = $gerrit::base,
  $userOwner    = $gerrit::userOwner,
  $userGroup    = $gerrit::userGroup,) {
  $envbase = "$base/$envid"
  $conf = "$base/conf.d"
  $envhost = regsubst(file("/etc/hostname"), '\n', '')
  $privateIp = "192.168.50.4"

  /* can't use cwd => $envbase since that may not yet exist and would cause a cyclic dependency */
  exec { "stop $envid":
    command => "/bin/sh -c '(cd $envbase && $envbase/bin/gerrit.sh stop)'",
    require => Gerrit["$version"],
    user    => "$gerrit::userOwner",
    onlyif  => "test -x $envbase/bin/gerrit.sh && $envbase/bin/gerrit.sh check | grep -q 'Gerrit running'",
  }

  exec { "clear $envid":
    command => "rm -rf $envbase",
    require => Exec["stop $envid"],
    user    => "$gerrit::userOwner",
    onlyif  => "test -e $base/clearexisting",
  }

  exec { "configure $envid":
    command => "/usr/lib/jvm/java-7-openjdk-i386/jre/bin/java -jar $base/archive/gerrit-$version.war init --batch --site-path $envbase --no-auto-start",
    require => Exec["clear $envid"],
    user    => "$gerrit::userOwner",
    creates => "$envbase",
  }

  file { "$envbase/etc/gerrit.config":
    content => template('gerrit/gerrit.config.erb'),
    require => Exec["configure $envid"],
  }

  file { "$envbase":
    ensure  => "directory",
    owner   => "$gerrit::userOwner",
    group   => "$gerrit::userGroup",
    require => Exec["configure $envid"]
  }

  if $digestauth {
    file { "$envbase/htpasswd.digest":
      content => template('gerrit/htpasswd.digest.erb'),
      owner   => "$gerrit::userOwner",
      group   => "$gerrit::userGroup",
      require => File["$envbase"],
    }
  } else {
    file { "$envbase/htpasswd":
      content => template('gerrit/htpasswd.erb'),
      owner   => "$gerrit::userOwner",
      group   => "$gerrit::userGroup",
      require => File["$envbase"],
    }
  }

  file { "$envbase/admin.id_rsa":
    source  => "puppet:///modules/gerrit/admin.id_rsa",
    owner   => "$gerrit::userOwner",
    group   => "$gerrit::userGroup",
    require => File["$envbase"],
  }

  gerrit::user { "admin user for $envid":
    username  => "admin",
    userid    => 1000000,
    useremail => "admin@egerrit.eclipse.org",
    userkey   => template('gerrit/admin.id_rsa.pub'),
    usergroup => "Administrators",
    base      => $base,
    envid     => $envid,
    require   => [Exec["configure $envid"],],
  }

  gerrit::user { "test1 user for $envid":
    username  => "test1",
    userid    => 1000001,
    useremail => "test1@egerrit.eclipse.org",
    userkey   => template('gerrit/test1.id_rsa.pub'),
    usergroup => "N/A",
    base      => $base,
    envid     => $envid,
    require   => [Exec["configure $envid"],]
  }

  gerrit::user { "test2 user for $envid":
    username  => "test2",
    userid    => 1000002,
    useremail => "test2@egerrit.eclipse.org",
    userkey   => template('gerrit/test2.id_rsa.pub'),
    usergroup => "N/A",
    base      => $base,
    envid     => $envid,
    require   => [Exec["configure $envid"],]
  }

  exec { "start $envid":
    command => "$envbase/bin/gerrit.sh start",
    cwd     => "$envbase",
    user    => "$gerrit::userOwner",
    require => [Gerrit::User["admin user for $envid"], Gerrit::User["test1 user for $envid"],  Gerrit::User["test2 user for $envid"], File["$envbase/etc/gerrit.config"]],
    creates => "$envbase/log/gerrit.pid",
  }

  $ssh = "ssh -p $sshport -i $envbase/admin.id_rsa -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no admin@localhost"

  exec { "create project for $envid":
    command => "$ssh gerrit create-project --name egerrit/test --empty-commit",
    #    user => "$gerrit::userOwner",
    require => [Exec["start $envid"], File["$envbase/admin.id_rsa"]],
    creates => "$envbase/git/egerrit/test.git"
  }
  
 exec { "create RCPTT project for $envid":
    command => "$ssh gerrit create-project --name egerrit/RCPTTtest --empty-commit",
    #    user => "$gerrit::userOwner",
    require => [Exec["start $envid"], File["$envbase/admin.id_rsa"]],
    creates => "$envbase/git/egerrit/RCPTTtest.git"
  }
 
}
