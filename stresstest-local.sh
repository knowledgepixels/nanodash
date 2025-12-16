#!/usr/bin/env bash

curl --silent https://registry.knowledgepixels.com/agents \
  | grep -Po 'orcid:[0-9-X]+' | sed 's|orcid:|https://orcid.org/|' \
  | sed -r 's_(.*)_echo -n "."; curl --silent -L --output /dev/null http://localhost:37373/user?id=\1_' \
  | bash

echo ""
echo "Finished"
