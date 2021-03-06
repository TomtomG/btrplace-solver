#!/bin/sh
#Script to notify the website about a release

function sedInPlace {
	if [ $(uname) = "Darwin" ]; then			
		sed -i '' "$1" $2
	else
		sed -i'' "$1" $2
	fi
}

if [ $# -ne 2 ]; then
    echo "Usage: $0 [site|code] version_number"
    echo "'site': notify a release on the website"
    echo "'code': upgrade the version number in the code"
    exit 1
fi
VERSION=$2
REPO_URL="http://btrp.inria.fr/repos"
APIDOC_URL="http://btrp.inria.fr/apidocs"

case $1	in

site)	
	WWW_HOOK="http://btrp.inria.fr/admin/bump_release.php"

	JSON="{\"version\":\"$VERSION\",\	
	\"title\":\"solver\",\
	\"apidoc\":\"$APIDOC_URL/releases/btrplace/solver/$VERSION/\",\
	\"changelog\":\"https://github.com/fhermeni/btrplace-solver/tree/btrplace-solver-$VERSION/CHANGES.md\",\
	\"binary\":\"$REPO_URL/releases/btrplace/solver-bundle/$VERSION/solver-bundle-$VERSION.jar\",\
	\"sources\":\"https://github.com/fhermeni/btrplace-solver/tree/btrplace-solver-$VERSION\"
	}"
	curl -X POST --data "data=$JSON" $WWW_HOOK
	;;
code)

	## The README.md
	# Update of the version number for maven usage	
		
	sedInPlace "s%<version>.*</version>%<version>$VERSION</version>%"  README.md
	
	snapshot=0
	echo $VERSION | grep "\-SNAPSHOT$" > /dev/null && snapshot=1

	if [ $snapshot = 0 ]; then 
		# Update the bundle and the apidoc location
		sedInPlace "s%$REPO_URL.*solver\-bundle.*%$REPO_URL/releases/btrplace/solver\-bundle/$VERSION/solver\-bundle\-$VERSION\.jar%" README.md
		sedInPlace "s%$APIDOC_URL/.*%$APIDOC_URL/releases/btrplace/solver/$VERSION/%" README.md
	else 
		# Update the bundle and the apidoc location
		sedInPlace "s%$REPO_URL.*solver\-bundle.*%$REPO_URL/snapshot-releases/btrplace/solver\-bundle/$VERSION/%" README.md	 #There is multiple jar for the snapshots, so we refer to the directory
		sedInPlace "s%$APIDOC_URL/.*%$APIDOC_URL/snapshots/btrplace/solver/%" README.md
	fi

	## The CHANGES.md file
	d=`LANG=en_US.utf8 date +"%d %b %Y"`
	REGEX="s%????*%${VERSION} - ${d}%"	
	sedInPlace "${REGEX}" CHANGES.md

	## The README.md inside examples
		if [ $snapshot = 0 ]; then
    		# Update the bundle and the apidoc location
    		sedInPlace "s%$REPO_URL.*solver\-examples.*%$REPO_URL/releases/btrplace/solver\-examples/$VERSION/solver\-examples\-$VERSION-dist\.tar.gz%" examples/README.md
    	else
    		# Update the bundle and the apidoc location
    		sedInPlace "s%$REPO_URL.*solver\-examples.*%$REPO_URL/snapshot-releases/btrplace/solver\-examples/$VERSION/%" examples/README.md #There is multiple jar for the snapshots, so we refer to the directory
    		sedInPlace "s%$APIDOC_URL/.*%$APIDOC_URL/snapshots/btrplace/solver/%" README.md
    	fi
	;;

*)
		echo "Target must be either 'site' or 'code'"
		exit 1
esac

