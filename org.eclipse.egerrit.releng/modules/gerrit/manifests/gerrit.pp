define gerrit::gerrit (
  $version   = "$title",
  $base      = $gerrit::base,
  $userOwner = $gerrit::userOwner,
  $userGroup = $gerrit::userGroup,
  $postfix   = "",) {
  include "gerrit"

  Exec {
    path => ["/bin/", "/sbin/", "/usr/bin/", "/usr/sbin/"]
  }

  notice (" In gerrit.pp, just before the prepare version: $version" )
  exec { "prepare $version":
    command => "mkdir -p $base/archive $base/conf.d",
    require => Exec["prepare gerrit"],
    user    => "$userOwner",
    creates => "$base/archive",
  }

  notice (" In gerrit.pp, just before the download version: $version" )
  exec { "download gerrit $version":
    command => "wget -O $base/archive/gerrit-$version.war http://gerrit-releases.storage.googleapis.com/gerrit${postfix}-${version}.war",
    creates => "$base/archive/gerrit-$version.war",
    user    => "$userOwner",
    require => Exec["prepare $version"],
  }

}
