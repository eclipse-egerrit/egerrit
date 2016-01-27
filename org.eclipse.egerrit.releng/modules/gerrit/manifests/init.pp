class gerrit {
  $base = "/home/tools/gerrit"
  $userOwner = "tools"
  $userGroup = "tools"

  exec { "apt-get update":
    command => "apt-get update",
    onlyif  => "find /var/lib/apt/lists/ -mtime -7 | (grep -q Package; [ $? != 0 ])",
  }

  $requirements = [ "openjdk-7-jre", "git-core",]

  package { $requirements:
    ensure  => "installed",
    require => Exec["apt-get update"],
  }


  exec { "prepare gerrit":
    command => "echo Gerrit pre-requisites are installed",
    require => Package[$requirements],
  }

}
