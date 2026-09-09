# Memory Contract

## What counts as memory

Memory is durable information likely to improve future conversations.

Good examples:
- stable user preferences;
- recurring goals;
- important personal facts explicitly shared by the user;
- named people/pets/projects when useful;
- user-requested reminders about preferences.

Do not automatically persist every conversational sentence.

## Memory extraction policy

After a response, the memory subsystem may propose candidates. Candidates must contain:
- category;
- concise text;
- reason;
- importance score;
- source message ID.

For v1, user-approved memory is the strongest persistence signal. The UI should make durable memory inspectable and removable.

## Retrieval policy

Retrieve a small number of relevant memories based on lexical match, importance and recency. Keep retrieved memory compact.

Never claim a memory exists when retrieval did not find it.

## Deletion

Delete must be real deletion from persistent storage. "Hide" is not equivalent to delete.
