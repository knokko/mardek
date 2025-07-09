 # Importer
 The `importer` module parses the MARDEK flash game (the .swf file),
 and converts it to a format that the rest of this MARDEK engine
 understands.
 
## The content
It will store the main [content](../content/README.md) in
`game/src/main/resources/mardek/game/content.bits`.
Furthermore, it puts the 'title-screen content' in
`title-screen.bits`, which is much less interesting.

## The GPU resources
All resources that should go to video memory will be stored in
`game/src/main/resources/mardek/game/content.vk2d`. This is
separated from `content.bits`, which makes it possible to send
all resources to video memory before the main content is parsed.
