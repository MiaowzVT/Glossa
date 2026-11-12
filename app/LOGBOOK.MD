# AI Usage - Logbook

## October 30th, 2026

### First Prompt

**Prompt**: <br>
The workspace you're in right now is the scaffolding for an Android App. Our goal is to create a full Android App out of this scaffolding. For this, we first want to implement one of    
many features: A "Grammar Library" - The user should be able to create new Grammar Entries for their Library by pressing a "+" button in the application's view. That will send them to   
another view, where they can create a short title, a detailed description and example sentences. For the implementation, we will also need support for RubyTextView, so the user can use  
for example Furigana when creating the Grammar Entries. I also want a Rich Text Editor for this view, so that the user can work with Colors, Text Edits (e.g. Bold, Italic, Underline)    
etc.. Finally, each grammar entry should be able to be categorized by a Language and a Sub-Category inside the Language.

**Changes**: <br>

### Second Prompt

<code>Note: This prompt happened in a new Claude Code Session. For some reason, Anthropic doesn't like it when you ask Claude to dump both Prompt and Answers from Claude into a text file, as that will trigger their safeguards. That eventually led to all my prompts being blocked, requiring me to create a new Session.</code>

**Prompt**: <br>
"The current workspace is an Android App. Write an Architecture Document in a style that you can work
with for additions on the code bae."

**Changes**: <br>

### Third Prompt

Based on Claude's question "At the moment I'd only see this file in a later session if I happened to open it. If you'd like it picked up automatically, I can add a short CLAUDE.md that points to it." after finishing the Architecture Document

**Prompt**: <br>
Yes, add a CLAUDE.md

**Changes** <br>
Added a CLAUDE.md in the project root, describing the rules for AI Agents to work on the project:
- ARCHITECTURE.md needs to be read before making any major changes
- ARCHITECTURE.md must be updated at all times
- 