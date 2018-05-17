LOCAL_ROOT=/music/Code/ej_backups
REMOTE_ROOT=/export/ed/backup
TAG=`date +'%Y-%m-%d'`

scp -P22022 inn@www.electricjungle.ru:$REMOTE_ROOT/ejungle-$TAG.sql.gz $LOCAL_ROOT/
scp -P22022 -r inn@www.electricjungle.ru:$REMOTE_ROOT/beings-$TAG  $LOCAL_ROOT/beings-$TAG
