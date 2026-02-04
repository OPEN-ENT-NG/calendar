#!/bin/bash

MVN_OPTS="-Duser.home=/var/maven"

if [ ! -e node_modules ]
then
  mkdir node_modules
fi

case `uname -s` in
  MINGW* | Darwin*)
    USER_UID=1000
    GROUP_UID=1000
    ;;
  *)
    if [ -z ${USER_UID:+x} ]
    then
      USER_UID=`id -u`
      GROUP_GID=`id -g`
    fi
esac

# Options
NO_DOCKER=""
SPRINGBOARD="recette"
for i in "$@"
do
case $i in
  -s=*|--springboard=*)
  SPRINGBOARD="${i#*=}"
  shift
  ;;
  --no-docker*)
  NO_DOCKER="true"
  shift
  ;;
  *)
  ;;
esac
done

init() {
  me=`id -u`:`id -g`
  echo "DEFAULT_DOCKER_USER=$me" > .env
  echo "" >> .env
  echo "# Vite proxy configuration" >> .env
  echo "#VITE_RECETTE= " >> .env
  echo "#VITE_ONE_SESSION_ID= " >> .env
  echo "#VITE_XSRF_TOKEN= " >> .env
}

clean () {
  echo "Cleaning front files"
  rm -rf .nx
  rm -rf .pnpm-store
  rm -rf node_modules 
  rm -f pnpm-lock.yaml
  rm -rf ./src/main/resources/public/dist 
  rm -rf ./src/main/resources/public/build 

  if [ "$NO_DOCKER" = "true" ] ; then
    mvn clean
  else
    docker compose run --rm maven mvn $MVN_OPTS clean
  fi
}

install () {
  docker compose run --rm maven mvn $MVN_OPTS install -DskipTests
}

test () {
  docker compose run --rm maven mvn $MVN_OPTS test
}

buildFrontend () {
  if [ ! -e "pnpm-lock.yaml" ] ; then
    echo "Running pnpm install..."
    if [ "$NO_DOCKER" = "true" ] ; then
      pnpm install
    else
      docker compose run -e NPM_TOKEN -e TIPTAP_PRO_TOKEN --rm -u "$USER_UID:$GROUP_GID" node sh -c "pnpm install"
    fi
  fi

  echo "Building frontend..."
  if [ "$NO_DOCKER" = "true" ] ; then
    pnpm run build
  else
    docker compose run -e NPM_TOKEN -e TIPTAP_PRO_TOKEN --rm -u "$USER_UID:$GROUP_GID" node sh -c "pnpm build"
  fi
  status=$?
  if [ $status != 0 ];
  then
    exit $status
  fi
}

publish() {
  version=`docker compose run --rm maven mvn $MVN_OPTS help:evaluate -Dexpression=project.version -q -DforceStdout`
  level=`echo $version | cut -d'-' -f3`
  case "$level" in
    *SNAPSHOT) export nexusRepository='snapshots' ;;
    *)         export nexusRepository='releases' ;;
  esac

  docker compose run --rm  maven mvn -DrepositoryId=ode-$nexusRepository -DskipTests --settings /var/maven/.m2/settings.xml deploy
}

publishNexus() {
  version=`docker compose run --rm maven mvn $MVN_OPTS help:evaluate -Dexpression=project.version -q -DforceStdout`
  level=`echo $version | cut -d'-' -f3`
  case "$level" in
    *SNAPSHOT) export nexusRepository='snapshots' ;;
    *)         export nexusRepository='releases' ;;
  esac
  docker compose run --rm  maven mvn -DrepositoryId=ode-$nexusRepository -Durl=$repo -DskipTests -Dmaven.test.skip=true --settings /var/maven/.m2/settings.xml deploy
}

watch () {
  if [ "$NO_DOCKER" = "true" ] ; then
    pnpm dev
  else
    docker compose run -e NPM_TOKEN -e TIPTAP_PRO_TOKEN --rm -u "$USER_UID:$GROUP_GID" node sh -c "pnpm dev"
# TODO appliquer la variable SPRINGBOARD comme fait auparavant :
#    docker compose run --rm -u "$USER_UID:$GROUP_GID" node sh -c "node_modules/gulp/bin/gulp.js watch --springboard=/home/node/$SPRINGBOARD"
  fi
}

for param in "$@"
do
  case $param in
    init)
      init
      ;;
    clean)
      clean
      ;;
    buildFrontend)
      buildFrontend
      ;;
    buildMaven)
      install
      ;;
    install)
      buildFrontend && install
      ;;
    test)
      test
      ;;
    watch)
      watch
      ;;
    publish)
      publish
      ;;
    publishNexus)
      publishNexus
      ;;
    *)
      echo "Invalid argument : $param"
  esac
  if [ ! $? -eq 0 ]; then
    exit 1
  fi
done

