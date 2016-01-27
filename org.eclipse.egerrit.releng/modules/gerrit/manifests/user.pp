define gerrit::user ($username, $userid, $useremail, $usergroup, $userkey, $envid, $base = $gerrit::base,) {
  $envbase = "$base/$envid"

  notice ("File: user.pp prepare to create a new file: envbase=${envbase}) ")

  file { "$envbase/adduser.${username}.sql":
    content => template('gerrit/adduser.sql.erb'),
    owner   => "$gerrit::userOwner",
    group   => "$gerrit::userGroup",
    require => File["$envbase"],
  }

  notice ("File: user.pp exec add user $username for $envid  ")

  exec { "add user $username for $envid":
    command => "/usr/lib/jvm/java-7-openjdk-i386/jre/bin/java -jar bin/gerrit.war gsql < $envbase/adduser.${username}.sql",
    cwd     => "$envbase",
    user    => "$gerrit::userOwner",
    require => [File["$envbase/adduser.${username}.sql"],],
  }

}
