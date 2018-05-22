PACKAGE=$1
PACKAGE_VERSION=$2
BUILD_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

echo $PACKAGE
echo $PACKAGE_VERSION
echo $BUILD_DIR

echo "Test fake package" | tee testfakepackage.deb
