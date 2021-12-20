#!/bin/bash

usage() {
	echo "Usage:"
	echo -e "  $0 [-h] [-s source] [-b backup] from to\n"
	echo "From the root of the AOSP source tree, backup files with committed changes"
	echo -e "among 'from' and 'to' commits.\n"
	echo "Options:"
	echo -e "  -b\tbackup directory (defaults to 'backup' directory within source directory)"
	echo -e "  -h\tdisplay help"
	echo -e "  -s\tsource tree directory (defaults to current directory)"
}

# TODO: stash and pop changes only for repositories with diff

while getopts ":hs:b:" flag
do
	case $flag in
		h|\?)
			usage
			exit
			;;
		s)
			source=$OPTARG
			;;
		b)
			backup=$OPTARG
			;;
	esac
done

# set source to current directory when not given
if [ -z $source ]; then
	source=$PWD
fi

# set backup to 'backup' directory within the source directory when not given
if [ -z $backup ]; then
	backup="$source/backup"
fi
backup=$(realpath -s $backup)

# remove options
shift $((OPTIND -1))

# ensure the right number of positional arguments are given
if [ $# -ne 2 ]; then
    usage
	exit
fi

# parse positional arguments
from=$1
to=$2

whereami=$PWD
cd "$source"
mkdir -p "$backup"

# move to stash changes in a dirty working directory
repo forall -c "git stash"

prefix=

while IFS= read -r line
do
	if [[ $line =~ project\ * ]]
	then
		prefix=$(echo $line | cut -c9-)
	elif [[ "$line" != "" ]]
	then
		dir=$(dirname "$prefix$line")
		mkdir -p "$backup/$dir"
		cp -u $prefix$line "$backup/$dir"
		echo "$prefix$line"
	fi
done <<< $(repo forall -cp "git diff --name-only $from..$to")

# restore stash on top of the current working directory
repo forall -c "git stash pop"

cd "$whereami"
