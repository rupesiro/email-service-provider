#!/usr/bin/env bash

### Functions ##########################################################################################################

function backup_file() {
  echo 'Backing up file'
  mv src/main/resources/public_suffix_list.dat src/main/resources/public_suffix_list.dat.backup
}

function download_latest_file() {
  echo 'Downloading latest version of the public suffix list'
  if (which wget &>/dev/null); then
    wget --quiet -O src/main/resources/public_suffix_list.dat https://publicsuffix.org/list/public_suffix_list.dat
  elif (which curl &>/dev/null); then
    curl --silent -o src/main/resources/public_suffix_list.dat https://publicsuffix.org/list/public_suffix_list.dat
  else
    echo 'Could not find either wget or curl'
    return 1
  fi
}

function cleanup_on_success() {
  echo 'Deleting backup file'
  rm -f src/main/resources/public_suffix_list.dat.backup
}

function file_is_updated() {
  ! (cmp src/main/resources/public_suffix_list.dat.backup src/main/resources/public_suffix_list.dat &>/dev/null)
}

function cleanup_on_failure() {
  echo 'Restoring backup file due to failure'
  if [[ -f src/main/resources/public_suffix_list.dat.backup ]]; then
    mv src/main/resources/public_suffix_list.dat.backup src/main/resources/public_suffix_list.dat
  fi
}

function update_file_on_repository() {
  git add src/main/resources/public_suffix_list.dat \
    && git commit -m "Update public suffix list ($(printf '%(%Y%m%d)T'))" \
    && git push
}

### Script Start #######################################################################################################

backup_file
if (download_latest_file); then
  if (file_is_updated); then
    echo 'Updated public suffix file'
    cleanup_on_success

    update_file_on_repository
  else
    echo 'Public suffix file already up-to-date'
    cleanup_on_success
  fi
else
  cleanup_on_failure
fi
