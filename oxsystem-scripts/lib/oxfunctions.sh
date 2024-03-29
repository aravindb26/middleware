#
#
#   @copyright Copyright (c) OX Software GmbH, Germany <info@open-xchange.com>
#   @license AGPL-3.0
#
#   This code is free software: you can redistribute it and/or modify
#   it under the terms of the GNU Affero General Public License as published by
#   the Free Software Foundation, either version 3 of the License, or
#   (at your option) any later version.
#
#   This program is distributed in the hope that it will be useful,
#   but WITHOUT ANY WARRANTY; without even the implied warranty of
#   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
#   GNU Affero General Public License for more details.
#
#   You should have received a copy of the GNU Affero General Public License
#   along with OX App Suite.  If not, see <https://www.gnu.org/licenses/agpl-3.0.txt>.
#
#   Any use of the work other than as authorized under this license or copyright law is prohibited.
#
#

# debian postinst is going to fail when not set'ting +e
set +e

# CentOS moves utils like pidof to /sbin so we have to append it to $PATH if
# not already contained
[[ "$PATH" =~ (^|:)/sbin:* ]] || PATH=${PATH}:/sbin

JAVA_BIN=

ox_set_JAVA_BIN() {
    JAVA_BIN=$(which java)
    if [ -z "$JAVA_BIN" ]; then
        local jb=$JAVA_HOME/bin/java
        if [ -x $jb ]; then
            JAVA_BIN=$jb
        fi
    fi
    if [ -z "$JAVA_BIN" ]; then
        local jb=$JRE_HOME/bin/java
        if [ -x $jb ]; then
            JAVA_BIN=$jb
        fi
    fi
    test -x "$JAVA_BIN" || die "$0: unable to get path to java vm"
    version=$(detect_java_version)
    if [ $version -lt 8 ]; then
      JAVA_BIN=/opt/open-xchange/sbin/insufficientjava
    fi
}

# Detect the version of the selected JVM
#
# Pre JEP 223:
# JVMs output e.g: java version "1.7.0_80" as part of their version
# specification. From this line we simply extract the minor version which would
# be 7 in this case.
#
# Post JEP 223:
# JVMs output e.g: java version "9-ea", "9" or "9.0.1" as part of their version
# specification. From this line we simply extract the major version which would
# be 9 in this case.
#
# Returns the detected version or -1 if it can't be detected
function detect_java_version () {
    version_line_array=( $($JAVA_BIN -version 2>&1 | grep version) )
    unquoted_version=${version_line_array[2]//\"/}
    version=-1
    if [[ "$unquoted_version" =~ ^1\..* ]]  
    then
        version_components=( ${unquoted_version//./ } )
        version=${version_components[1]}
    elif [[ "$unquoted_version" =~ ^[1-9]([0-9])*-ea$ ]]  
    then
        version_components=( ${unquoted_version//./ } )
        version=${unquoted_version//-ea/}
    elif [[ "$unquoted_version" =~ ^[1-9]([0-9])*(\..*)* ]]  
    then
        version_components=( ${unquoted_version//./ } )
        version=${version_components[0]}
    fi
    echo $version
}

DEBIAN=1
REDHAT=2
SUSE=4
LSB=8
UCS=16
ox_system_type() {
    local ret=0
    local isucs=$(uname -r|grep ucs)
    if [ -f /etc/debian_version ] && [ -z "$isucs" ]; then
        ret=$(( $ret | $DEBIAN ))
    elif [ -n "$isucs" ]; then
        ret=$(( $ret | $UCS))
    elif [ -f /etc/SuSE-release ]; then
        ret=$(( $ret | $SUSE ))
        ret=$(( $ret | $LSB ))
    elif [ -f /etc/redhat-release ]; then
        ret=$(( $ret | $REDHAT ))
        ret=$(( $ret | $LSB ))
    fi
    return $ret
}

# init script stuff

ox_start_daemon() {
    local path="$1"
    local name="$2"
    local user="$3"
    local group="$4"
    test -z "$path" && die "ox_start_daemon: missing path argument (arg 1)"
    test -x $path   || die "ox_start_daemon: $path is not executable"
    test -z "$name" && die "ox_start_daemon: missing name argument (arg 2)"
    local runasuser=
    test -n "$user"   && runasuser="--chuid $user"
    local runasgroup=
    test -n "$group"  && runasgroup="--group $group"
    ox_system_type
    local type=$?
    if [ $type -eq $DEBIAN -o $type -eq $UCS ]; then
        start-stop-daemon $runasuser $runasgroup --background --start --oknodo --startas $path --make-pidfile --pidfile /var/run/${name}.pid
    elif [ $(( $type & $LSB )) -eq $LSB ]; then
        if [ -n "$user" ] && [ "$user" != "root" ]; then
            su -s /bin/bash $user -c $path > /dev/null 2>&1 & echo $! > /var/run/${name}.pid
        else
            $path > /dev/null 2>&1 & echo $! > /var/run/${name}.pid
        fi
    else
        die "Unable to handle unknown system type"
    fi
}

ox_is_running() {
    local name="$1"
    local pattern="$2"
    local pid="$3"
    test -z "$name" && die "ox_is_running: missing name argument (arg 1)"
    test -z "$pattern" && die "ox_is_running: missing pattern argument (arg 2)"

    if [ -z "$pid" ]; then
       if [ -e /var/run/${name}.pid ]; then
          read pid < /var/run/${name}.pid
       fi
    fi
    if [ -n "$pid" ]; then
        # take care nothing influences line length if ps output
        COLUMNS=1000
        if ps $pid | grep "$pattern" > /dev/null; then
           return 0
        else
           return 1
        fi
    else
        return 1
    fi
}

ox_stop_daemon() {
    local name="$1"
    local nonox="$2"
    test -z "$name" && die "ox_stop_daemon: missing name argument (arg 1)"
    ox_system_type
    local type=$?

    if [ ! -f /var/run/${name}.pid ]; then
        return 0
    fi
    read PID < /var/run/${name}.pid
    test -z "$PID" && { echo "No process in pidfile '/var/run/${name}.pid' found running; none killed."; return 1; }
    if [ -z "$nonox" ]; then
        ps $PID > /dev/null && /opt/open-xchange/sbin/shutdown -w > /dev/null 2>&1
    fi
    ps $PID > /dev/null && kill -QUIT $PID
    ps $PID > /dev/null && kill -TERM $PID
    rm -f /var/run/${name}.pid
}

ox_daemon_status() {
    local pidfile="$1"
    test -z "$pidfile" && die "ox_daemon_status: missing pidfile argument (arg 1)"
    if [ ! -f $pidfile ]; then
        # not running
        return 1
    fi
    read PID < $pidfile
    running=$(ps $PID | grep $PID)
    if [ -n "$running" ]; then
        # running
        return 0
    else
        # not running
        return 1
    fi
}

# Scans for expression 1 in the file given as parameter2
#
# Param1: The expression to look for
# Param2: The file to search in
# Return: 0 if the expression was found, 1 otherwise
# Example:
#
#   root@host:~$ $(contains SERVER_NAME /opt/open-xchange/etc/system.properties)
#   && echo yes || echo no
#   yes
#   root@host:~$ 
#
contains() {
    local expression="$1"
    local file="$2"
    test -z "$expression" && die "contains: missing expression argument (arg 1)"
    test -z "$file" && die "contains: missing file argument (arg 2)"
    test -e "$file" || die "contains: $file does not exist"

    grep -q -- "$expression" "$file"
    return $?
}

# Calculate and return only the md5sum of the given file
# Param1: The (prop)file to calculate the md5sum from
# Return: The md5 sum of the file given as Param1 
# Example:
#
#   root@host:~$ [[ $(ox_md5 /usr/bin/md5sum) = $(ox_md5 /usr/bin/md5sum) ]] && echo equal || echo differ
#   equal
#   root@host:~$ 
#
function ox_md5() {
    local file="$1"
    test -z "$file" && die "ox_md5: missing file argument (arg 1)"
    test -e "$file" || die "ox_md5: $file does not exist"
    local output=$(md5sum $file)
    local md5=${output%% *}
    echo $md5
}

# usage:
# ox_set_property property value /path/to/file
#
ox_set_property() {
    local prop="$1"
    local val="$2"
    local propfile="$3"
    test -z "$prop"     && die "ox_set_property: missing prop argument (arg 1)"
    test -z "$propfile" && die "ox_set_property: missing propfile argument (arg 3)"
    test -e "$propfile" || die "ox_set_property: $propfile does not exist"
    local tmp=${propfile}.tmp$$
    cp -a --remove-destination $propfile $tmp

    ox_system_type
    local type=$?
    if [ $type -eq $DEBIAN -o $type -eq $UCS ]; then
        local origfile="${propfile}.dpkg-new"
        if [ ! -e $origfile ]; then
            local origfile="${propfile}.dpkg-dist"
        fi
    else
        local origfile="${propfile}.rpmnew"
    fi
    if [ -n "$origfile" ] && [ -e "$origfile" ]; then
        export origfile
        export propfile
        export prop
        export val

        perl -e '
use strict;

open(IN,"$ENV{origfile}") || die "unable to open $ENV{origfile}: $!";
open(OUT,"$ENV{propfile}") || die "unable to open $ENV{propfile}: $!";

my @LINES = <IN>;
my @OUTLINES = <OUT>;

my $opt = $ENV{prop};
my $val = $ENV{val};
my $count = 0;
my $back  = 1;
my $out = "";
foreach my $line (@LINES) {
    if ( $line =~ /^$opt\s*[:=]/ ) {
      $out = $line;
      $out =~ s/^(.*?[:=]).*$/$1$val/;
      while ( $LINES[$count-$back] =~ /^#/ ) {
          $out = $LINES[$count-$back++].$out;
      }
    }
    $count++;
}

$back  = 0;
$count = 0;

# either the line where the comments above the property start or the line where
# the matching property was found (end)
my $start = 0;

# the line where we found the matching property
my $end = 0;

# > 0 if found
my $found = 0;
foreach my $line (@OUTLINES) {
    # we can not properly match commented out properties, they might be contained
    # in comments themselves
    if ( $line =~ /^$opt\s*[:=]/ ) {
        # we got a match
        $found=1;

        # set end to the line where we found the match
        $end=$count;

        # increase back while lines above are comments
        while ( $OUTLINES[$count-++$back] =~ /^#/ ) {
        }
        ;
        # if we found at least one comment line start at the comments otherwise
        # start at the property
        if ( $count > 0 && $back > 1 ) {
            $start=$count-$back+1;
        } else {
            $start=$end;
        }
    }
    $count++;
}

#if we did not find the property set it to provided values
if ( length($out) == 0 ) {
    $out=$opt."=".$val."\n";
}

if ( $found ) {
    for (my $i=0; $i<=$#OUTLINES; $i++) {
        if ( $i < $start || $i > $end ) {
            print $OUTLINES[$i];
            print "\n" if( substr($OUTLINES[$i],-1) ne "\n" );
        }
        if ( $i == $start ) {
            # add newline unless first line or line above is emtpy
            if ($i > 0 && $OUTLINES[$i-1] !~ /^\s*$/) {
              print "\n";
            }
            print $out;
            print "\n" if( substr($OUTLINES[$i],-1) ne "\n" );
        }
    }
} else {
    print @OUTLINES;
    print "\n" if( substr($OUTLINES[-1],-1) ne "\n" );
    # add newline unless line above is emtpy
    if ($OUTLINES[-1] !~ /^\s*$/) {
      print "\n";
    }
    print $out;
    print "\n";
}
' > $tmp
        if [ $? -gt 0 ]; then
            rm -f $tmp
            die "ox_set_property: FATAL: error setting property $prop to \"$val\" in $propfile"
        else
            if [[ $(ox_md5 $tmp) != $(ox_md5 $propfile) ]]; then
              mv $tmp $propfile
            else
              rm $tmp
            fi
        fi
        unset origfile
        unset propfile
        unset prop
        unset val
    else
        # quote & in URLs to make sed happy
        test -n "$val" && val="$(echo $val | sed 's/\&/\\\&/g')"
        if grep -E "^$prop *[:=]" $propfile >/dev/null; then
            cat<<EOF | sed -f - $propfile > $tmp
s;\(^$prop[[:space:]]*[:=]\).*$;\1${val};
EOF
        else
        # add a newline to the last line if it doesn't exist
        sed -i -e '$a\' $tmp
            echo "${prop}=$val" >> $tmp
        fi
        if [ $? -gt 0 ]; then
            rm -f $tmp
            die "ox_set_property: FATAL: error setting property $prop to \"$val\" in $propfile"
        else
            if [[ $(ox_md5 $tmp) != $(ox_md5 $propfile) ]]; then
              mv $tmp $propfile
            else
              rm $tmp
            fi
        fi
    fi
}

# usage:
# ox_exists_property property /path/to/file
#
ox_exists_property() {
    local prop="$1"
    local propfile="$2"
    test -z "$prop"     && die "ox_exists_property: missing prop argument (arg 1)"
    test -z "$propfile" && die "ox_exists_property: missing propfile argument (arg 2)"
    test -e "$propfile" || die "ox_exists_property: $propfile does not exist"

    local escaped=$(sed 's/[]\.|$(){}?+*^[]/\\&/g' <<< "$prop")
    grep -E "^$escaped *[:=]" $propfile >/dev/null || return 1
}

# savely find key/val in keys and values containing all kind of ugly chars
# delimiter must be either = or :
save_read_prop() {
    export prop="$1"
    export propfile="$2"
    perl -e '
use strict;

my $file=$ENV{"propfile"};
my $search=$ENV{"prop"};
open(FILE,$file) || die "unable to open $file: $!";
my $val=undef;
while(<FILE>) {
    chomp;
    my $len=length($search);
    if( substr($_,0,$len) eq $search ) {
        if( substr($_,$len,$len+1) !~ /^[\s=:]/ ) {
           next;
        }
        foreach my $dl ( "=", ":" ) {
           my $idx=index($_,$dl);
           if( $idx >= $len ) {
              $val=substr($_,$idx+1);
           }
           last if defined($val);
        }
        last;
    }
}
print "$val\n";

close(FILE);
'
}

# usage:
# ox_read_property property /path/to/file
#
ox_read_property() {
    local prop="$1"
    local propfile="$2"
    test -z "$prop"     && die "ox_read_property: missing prop argument (arg 1)"
    test -z "$propfile" && die "ox_read_property: missing propfile argument (arg 2)"
    test -e "$propfile" || die "ox_read_property: $propfile does not exist"

    # sed -n -e "/^$prop/Is;^$prop *[:=]\(.*\).*$;\1;p" < $propfile
    # UGLY: we have keys containing /
    save_read_prop "$prop" "$propfile"
}

# usage:
# ox_remove_property property /path/to/file
#
ox_remove_property() {
    local prop="$1"
    local propfile="$2"
    test -z "$prop"     && die "ox_remove_property: missing prop argument (arg 1)"
    test -z "$propfile" && die "ox_remove_property: missing propfile argument (arg 2)"
    test -e "$propfile" || die "ox_remove_property: $propfile does not exist"

    if ox_exists_property "$prop" "$propfile"; then
        local tmp=${propfile}.tmp$$
        cp -a --remove-destination $propfile $tmp

        export propfile
        export prop
        perl -e '
use strict;

open(IN,"$ENV{propfile}") || die "unable to open $ENV{propfile}: $!";

my @LINES = <IN>;

my $opt = $ENV{prop};
my $count = 0;
my $back  = 1;
my $start = 0;
my $end = 0;
foreach my $line (@LINES) {
    if ( $line =~ /^$opt\s*[:=]/ ) {
        $end=$count;
        while ( $LINES[$count-$back++] =~ /^#/ ) {
        }
        $start=$count-$back;
    }
    $count++;
}
if ( $LINES[$end+1] =~ /^\s*$/ ) {
    $end++;
}
for (my $i=0; $i<=$#LINES; $i++) {
    if ( $i <= $start+1 || $i > $end ) {
        print $LINES[$i];
    }
}
' > $tmp
        if [ $? -gt 0 ]; then
            rm -f $tmp
            die "ox_remove_property: FATAL: error removing property $prop from $propfile"
        else
            mv $tmp $propfile
        fi
    fi
    unset propfile
    unset prop
}

# adding or removing comment (ONLY # supported)
#
# usage:
# ox_comment property action /path/to/file
# where action can be add/remove
#
ox_comment(){
    local prop="$1"
    local action="$2"
    local propfile="$3"
    test -z "$prop"     && die "ox_comment: missing prop argument (arg 1)"
    test -z "$action"      && die "ox_comment: missing action argument (arg 2)"
    test -z "$propfile" && die "ox_comment: missing propfile argument (arg 3)"
    test -e "$propfile" || die "ox_comment: $propfile does not exist"
    local tmp=${propfile}.tmp$$
    local prop_in=$(quote_s_in $prop)
    local prop_re=$(quote_s_re $prop)
    cp -a --remove-destination $propfile $tmp
    if [ "$action" == "add" ]; then
        sed "s/^$prop_in/# $prop_re/" < $propfile > $tmp;
        if [ $? -gt 0 ]; then
            rm -f $tmp
            die "ox_comment: FATAL: could not add comment in file $propfile to $prop"
        else
            mv $tmp $propfile
        fi
    elif [ "$action" == "remove" ];then
      sed "s/^#[ ]*\($prop_in[ ]*=\)/\1/" < $propfile > $tmp;
        if [ $? -gt 0 ]; then
            rm -f $tmp
            die "ox_comment: FATAL: could not remove comment in file $propfile for $prop"
        else
            mv $tmp $propfile
        fi
    else
        die "ox_handle_hash: action must be add or remove while it is $action"
    fi
}

ox_update_permissions(){
    local pfile="$1"
    local owner="$2"
    local mode="$3"
    test -z "$pfile" && die "ox_update_permissions: missing pfile argument"
    test -z "$owner" && die "ox_update_permissions: missing owner argument"
    test -z "$mode" && die "ox_update_permissions: missing mode argument"
    test -e "$pfile" || die "ox_update_permissions: $pfile does not exist"

    chmod $mode "$pfile"
    chown $owner "$pfile"
}

die() {
    test -n "$1" && echo 1>&2 "$1" || echo 1>&2 "ERROR"
    exit 1
}

ox_update_config_ini() {
    local cini=$1
    local cinitemplate=$2
    local bdir=$3

    test -z "$cini" && die \
        "ox_update_config_init: missing config.ini argument (arg 1)"
    test -z "$cinitemplate" && die \
        "ox_update_config_init: missing config.ini template argument (arg 2)"
    test -z "$bdir" && die \
        "ox_update_config_init: missing bundle.d argument (arg 3)"

    test -d $bdir || die "$bdir is not a directory"
    test -f $cinitemplate || die "$cinitemplate does not exist"
    test "$(echo $bdir/*.ini)" == "$bdir/*.ini" && die "$bdir is empty"

    # read all installed bundles into an array
    local dirbundles=()
    local bpath=
    for bundle in $bdir/*.ini; do
        read bpath < $bundle
        dirbundles=( ${dirbundles[*]} "reference\:file\:${bpath}" )
    done

    if [ -f $cini ]; then
        # read all bundles listed in config.ini into an array
        local configbundles=( $(sed -e \
            '/^osgi.bundles.*/Is;^osgi.bundles=\(.*\);\1;' \
            -n -e 's;,; ;gp' < $cini ) )
    fi

    cp $cinitemplate $cini
    echo "osgi.bundles=$(echo ${dirbundles[@]} | sed 's; ;,;g')" >> $cini
}

# Similar to ox_update_config_ini but expects the ini files to be installed in separated folders (packages) and that a white- or blacklist is present to filter these packages
ox_update_cloud_config_ini() {
    local cini=$1
    local cinitemplate=$2
    local bdir=$3

    test -z "$cini" && die \
        "ox_update_config_init: missing config.ini argument (arg 1)"
    test -z "$cinitemplate" && die \
        "ox_update_config_init: missing config.ini template argument (arg 2)"
    test -z "$bdir" && die \
        "ox_update_config_init: missing bundle.d argument (arg 3)"

    test -d $bdir || die "$bdir is not a directory"
    test -f $cinitemplate || die "$cinitemplate does not exist"
    test -z "$OX_WHITELISTED_PACKAGES" && test -z "$OX_BLACKLISTED_PACKAGES" && die "Missing both OX_BLACKLISTED_PACKAGES and OX_WHITELISTED_PACKAGES"
    # read all installed bundles into an array
    local dirbundles=()
    local bpath=
    # If OX_WHITELISTED_PACKAGES is set and not empty only use whitelisted packages
    if [ -z "$OX_WHITELISTED_PACKAGES" ]
    then
        echo "Using all but blacklisted packages: $OX_BLACKLISTED_PACKAGES"
        for package in $bdir/*/; do
            # Ignore all blacklisted bundles
            for blacklisted in $OX_BLACKLISTED_PACKAGES; do
                local foldername="$(basename -- $package)"
                if [ ${foldername} == ${blacklisted} ]
                then
                    echo "Skipping blacklisted package ${package}"
                    continue 2
                fi
            done
            for bundle in $package/*.ini; do
                read bpath < $bundle
                dirbundles=( ${dirbundles[*]} "reference\:file\:${bpath}" )
            done
        done
    else
        echo "Using whitelisted bundles: $OX_WHITELISTED_PACKAGES"
        for package in $bdir/*/; do
            local skipped=1
            for whitelisted in $OX_WHITELISTED_PACKAGES; do
                local foldername="$(basename -- $package)"
                if [ ${foldername} == ${whitelisted} ]
                then
                    for bundle in $package/*.ini; do
                        read bpath < $bundle
                        dirbundles=( ${dirbundles[*]} "reference\:file\:${bpath}" )
                    done
                    skipped=0
                fi
            done
            if [ $skipped ]
            then
                echo "Skipping not whitelisted package ${package}"
            fi
        done
    fi

    if [ -f $cini ]; then
        # read all bundles listed in config.ini into an array
        local configbundles=( $(sed -e \
            '/^osgi.bundles.*/Is;^osgi.bundles=\(.*\);\1;' \
            -n -e 's;,; ;gp' < $cini ) )
    fi

    [ -z "$dirbundles" ] && die "No bundles found"

    cp $cinitemplate $cini
    echo "osgi.bundles=$(echo ${dirbundles[@]} | sed 's; ;,;g')" >> $cini
}

ox_save_backup() {
    local name=$1
    test -z "$name" && die "ox_save_backup: missing name argument (arg1)"

    local backup_name="${name}.old"
    if [ -e $name ]; then
        mv $name $backup_name
    fi
}

# move configuration file from one location/package to another
# RPM ONLY!
ox_move_config_file() {
    local srcdir="$1"
    local dstdir="$2"
    local srcname="$3"
    local dstname="$4"
    test -z "$srcdir" && die "ox_move_config_file: missing srcdir argument (arg1)"
    test -z "$dstdir" && die "ox_move_config_file: missing dstdir argument (arg2)"
    test -z "$srcname" && die "ox_move_config_file: missing srcname argument (arg3)"
    test -z "$dstname" && dstname=$srcname

    if [ -e "${srcdir}/${srcname}" ]; then
        if [ -e "${dstdir}/${dstname}" ] && \
           ! cmp -s "${dstdir}/${dstname}" "${srcdir}/${srcname}" > /dev/null; then
           mv "${dstdir}/${dstname}" "${dstdir}/${dstname}.rpmnew"
        fi
        mv "${srcdir}/${srcname}" "${dstdir}/${dstname}"
    fi
}

# kill all leftover readerengine instances from a previous start
ox_kill_readerengine_instances() {
    local programname="soffice.bin"

    for PID in $(pidof ${programname}); do
        if ! ps ${PID} > /dev/null; then
            return 0
        fi

        kill -KILL ${PID}
    done

    rm -f /tmp/OSL_PIPE_*
}

# ox_add_property property value /path/to/file
# verifies first that the property does not already exist in file and adds it then
ox_add_property() {
    local property="$1"
    local value="$2"
    local propfile="$3"
    test -z "$property" && die "ox_add_property: missing property argument (arg 1)"
    test -z "$propfile" && die "ox_add_property: missing propfile argument (arg 3)"
    test -e "$propfile" || die "ox_add_property: $propfile does not exist"

    if ! ox_exists_property "$property" "$propfile"
    then
        ox_set_property "$property" "$value" "$propfile"
    fi
}

# quote for sed s-command input as in: s/input/replacement/
# by prefixing each character of the character set "]\/$*.^|[" with a "\"
# and thus escaping them
quote_s_in () {
  sed -e 's/[]\/$*.^|[]/\\&/g' <<< "$1"
}

# quote for sed s-command replacement as in: s/input/replacement/
# by prefixing "\", "/" and "&" with a "\" and thus escaping 
#the backslash itself, the default s-command separator and the matched string
quote_s_re () {
  sed -e 's/[\/&]/\\&/g' <<< "$1"
}

# Scans the file given as parameter1 for uncommentented lines starting with
# either the old style JAVA_XTRAOPTS or the post RM-177 style JAVA_OPTS_MEM for
# the JVM max heap size configuration option given as either -Xmx or
# -XX:MaxHeapSize.
#
# Param1: The file to read the maximum heap size from
# Return: The first found config option for the JVM's max heap size or "n/a"
#         if either the file can't be read or the option can't be found
# Example:
#
#   root@host:~$ max_heap=$(ox_get_max_heap /opt/open-xchange/etc/ox-scriptconf.sh)
#   root@host:~$ echo ${max_heap}
#   root@host:~$ 512M
#
ox_get_max_heap() {
  local config_file="${1}"
  test -z "${config_file}" && die "ox_get_max_heap: missing config file argument (arg 1)"
  if [ -r "${config_file}" ]
  then
    local max_heap=$(awk '/^JAVA_XTRAOPTS|^JAVA_OPTS_MEM/ {
      gsub("JAVA.[^\"]*","")
      gsub("\"","")
      num_opts = split($0, mem_opts, " ")
      for (x=1; x <= num_opts; ++x) {
        if (mem_opts[x] ~ /-Xmx/) {
          gsub("-Xmx","", mem_opts[x])
          print mem_opts[x]
          exit
        } else if (mem_opts[x] ~ /-XX:MaxHeapSize/) {
          split(mem_opts[x], heap_size , "=")
          print heap_size[2]
          exit
        }
      }
    }' "${config_file}")
  fi
  local max_heap=${max_heap:-"n/a"}
  echo "${max_heap}"
}

# Scans the file given as parameter1 for uncommentented lines starting with
# either the old style JAVA_XTRAOPTS or the post RM-177 style JAVA_OPTS_MEM for
# the JVM max heap size configuration option given as either -Xmx or
# -XX:MaxHeapSize and sets it to the new value given as parameter2
#
# Param1: The file to set the maximum heap size in
# Param2: The new maximum heap size e.g. 768M or 2G
# Return: 0 if the new value was set, 1 otherwise
# Example:
#
#   root@host:~$ $(ox_set_max_heap /opt/open-xchange/etc/ox-scriptconf.sh 2G)
#   && echo set new value
#   set new value
#   root@host:~$ 
#
ox_set_max_heap() {
  local config_file="${1}"
  local new_value="${2}"
  test -z "${config_file}" && die "ox_set_max_heap: missing config file argument (arg 1)"
  test -z "${new_value}" && die "ox_set_max_heap: missing new value argument (arg 2)"

  # read
  local opts_line=$(ox_read_property JAVA_XTRAOPTS "${config_file}")
  if [[ -z "${opts_line}" ]]
  then
    opts_line=$(ox_read_property JAVA_OPTS_MEM "${config_file}")
    local new_style=1
  fi
  [[ -z "${opts_line}" ]] && return 1

  # unquote
  local opts=($(sed -e 's/\"//g' <<< "${opts_line}"))

  # modify
  local num_opts=${#opts[@]}
  for ((i=0; i < num_opts; i++))
  do
    if [[ "${opts[$i]}" =~ -XX:MaxHeapSize ]]
    then
      opts[$i]="-XX:MaxHeapSize=${new_value}"
      local modified=1
      break
    elif [[ "${opts[${i}]}" =~ -Xmx ]]
    then
      opts[$i]="-Xmx${new_value}"
      local modified=1
      break
    fi
  done

  ((modified)) || return 1

  # quote
  new_opts_line=\"${opts[*]}\"

  # persist
  if ((new_style))
  then
    ox_set_property JAVA_OPTS_MEM "${new_opts_line}" "${config_file}"
  else
    ox_set_property JAVA_XTRAOPTS "${new_opts_line}" "${config_file}"
  fi
  return $?
}

OX_SCR_DB=/opt/open-xchange/etc/scr_db
# Check if a SCR has to be run
# Example usage:
# if ox_scr_todo 1234
# then
#   whatever_is_requested_in_scr_1234
#   ox_scr_done 1234
# fi
ox_scr_todo() {
  local scr="$1"
  test -z "$scr" && die "ox_check_scr: missing property argument (arg 1)"
  if $(grep "^${scr}$" ${OX_SCR_DB} &> /dev/null)
  then
    return 1
  else
    return 0
  fi
}

# Mark the SCR as already run
ox_scr_done() {
  local scr="$1"
  test -z "$scr" && die "ox_scr_done: missing property argument (arg 1)"
  if [ ! -d $(dirname ${OX_SCR_DB}) ]
  then
    mkdir -p $(dirname ${OX_SCR_DB})
  fi
  $(ox_scr_todo ${scr}) && echo "${scr}" >> ${OX_SCR_DB}
}
