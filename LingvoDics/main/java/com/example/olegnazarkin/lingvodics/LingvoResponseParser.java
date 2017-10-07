package com.example.olegnazarkin.lingvodics;

import android.os.AsyncTask;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

class LingvoResponseParser
{
    public interface LingvoDictionaryEntryHandler
    {
        void consume(RootCollection rootCollection);
    }

    public interface MarkupSequence
    {
        String buildHtml();
    }

    public class EntryNode implements MarkupSequence
    {
        public String Content;

        public boolean IsItalics;
        public boolean IsAccent;

        public EntryNode(String content, boolean isItalics, boolean isAccent)
        {
            Content = content;
            IsItalics = isItalics;
            IsAccent = isAccent;
        }

        public String buildHtml()
        {
            String italicsDecorationStart = IsItalics ? "<i>" : "";
            String italicsDecorationEnd = IsItalics ? "</i>" : "";

            String accentDecoration = IsAccent ? "\u0301" : "";

            return "<span class=\"entry\">" + italicsDecorationStart +
                        Content + accentDecoration + italicsDecorationEnd + "</span>";
        }
    }

    public class CollectionNode implements MarkupSequence
    {
        public List<ArticleNode> Items = new ArrayList<>();

        public CollectionNode(JSONArray tryItems)
        {
            int length = tryItems.length();

            for(int i = 0; i < length; ++i)
            {
                try
                {
                    Items.add(new ArticleNode(tryItems.getJSONObject(i)));
                }
                catch(JSONException e)
                {}
            }
        }

        public String buildHtml()
        {
            int length = Items.size();

            boolean ordered = (length > 1);

            String s = ordered ? "<ol class=\"collection\">" : "<ul>";

            for(int i = 0; i < length; ++i)
            {
                s += "<li class=\"item\">" + Items.get(i).buildHtml() + "</li>";
            }

            return s + (ordered ? "</ol>" : "</ul>");
        }
    }

    public class MarkupNode implements MarkupSequence
    {
        public List<ArticleNode> Markup = new ArrayList<>();

        public MarkupNode(JSONArray tryMarkup)
        {
            try
            {
                int length = tryMarkup.length();

                for(int i = 0; i < length; ++i)
                {
                    Markup.add(new ArticleNode(tryMarkup.getJSONObject(i)));
                }
            }
            catch(JSONException e)
            {}
        }

        public String buildHtml()
        {
            String s = "<span class=\"markup\">";

            int length = Markup.size();

            for(int i = 0; i < length; ++i)
            {
                s += Markup.get(i).buildHtml();
            }

            return s + "</span>";
        }
    }

    public class CaptionNode extends EntryNode
    {
        public CaptionNode(String caption)
        {
            super(caption, false, false);
        }

        // to do HTML
        @Override
        public String buildHtml()
        {
            return "<span class=\"caption\">" + super.buildHtml() + "&nbsp;</span>";
        }
    }

    public class SoundNode extends EntryNode
    {
        public SoundNode(String fileName)
        {
            super(fileName, true, false);
        }

        @Override
        public String buildHtml()
        {
            return "<span class=\"sound\">" + super.buildHtml() + "</span>";
        }
    }

    public class TranscriptionNode extends EntryNode
    {
        public TranscriptionNode(String transcription)
        {
            super(transcription, false, false);
        }

        @Override
        public String buildHtml()
        {
            return "<span class=\"transcription\"><span>[</span>" + super.buildHtml() + "<span>]</span></span>";
        }
    }

    public class AbbrevNode extends EntryNode
    {
        public String FullText;

        public AbbrevNode(String text, String fullText)
        {
            super(text, false, false);

            FullText = fullText;
        }

        @Override
        public String buildHtml()
        {
            return "<span class=\"abbrev\">" + Content + "</span><span>&nbsp;</span>";
        }
    }

    public class CardRefNode extends EntryNode
    {
        public CardRefNode(String text, boolean isItalics, boolean isAccent)
        {
            super(text, isItalics, isAccent);
        }

        @Override
        public String buildHtml()
        {
            return "<a class=\"cardref\">" + Content + "</a><span>&nbsp;</span>";
        }
    }

    public class CommentNode extends MarkupNode
    {
        public CommentNode(JSONArray tryMarkup)
        {
            super(tryMarkup);
        }

        @Override
        public String buildHtml()
        {
            return "<span class=\"comment\">" + super.buildHtml() + "</span>";
        }
    }

    public class ExampleNode extends MarkupNode
    {
        public ExampleNode(JSONArray tryMarkup)
        {
            super(tryMarkup);
        }

        @Override
        public String buildHtml()
        {
            return "<span class=\"example\">" + super.buildHtml() + "</span>";
        }
    }

    public class ParagraphNode extends MarkupNode
    {
        public ParagraphNode(JSONArray tryMarkup)
        {
            super(tryMarkup);
        }

        @Override
        public String buildHtml()
        {
            return "<span class=\"paragraph\">" + super.buildHtml() + "</span><br>";
        }
    }

    public class ExampleItemNode extends MarkupNode
    {
        public ExampleItemNode(JSONArray tryMarkup)
        {
            super(tryMarkup);
        }

        @Override
        public String buildHtml()
        {
            return "<span class=\"exampleitem\">" + super.buildHtml() + "</span>";
        }
    }

    public class CardRefItemNode extends MarkupNode
    {
        public CardRefItemNode(JSONArray tryMarkup)
        {
            super(tryMarkup);
        }

        @Override
        public String buildHtml()
        {
            return "<a class=\"cardrefitem\">" + super.buildHtml() + "</a>";
        }
    }

    public class ExamplesListNode extends CollectionNode
    {
        public ExamplesListNode(JSONArray tryItems)
        {
            super(tryItems);
        }

        @Override
        public String buildHtml()
        {
            int length = Items.size();

            String s = "<div class=\"examples\">";

            for(int i = 0; i < length; ++i)
            {
                s += Items.get(i).buildHtml() + "<br>";
            }

            return s + "</div>";
        }
    }

    public class CardRefsListNode extends CollectionNode
    {
        public CardRefsListNode(JSONArray tryItems)
        {
            super(tryItems);
        }

        @Override
        public String buildHtml()
        {
            int length = Items.size();

            String s = "<div class=\"cardrefs\">";

            for(int i = 0; i < length; ++i)
            {
                s += Items.get(i).buildHtml() + "<br>";
            }

            return s + "</div>";
        }
    }

    public class ArticleNode implements MarkupSequence
    {
        public String Node;
        public String Text;
        public boolean IsOptional;

        public EntryNode Entry;
        public MarkupNode Markup;
        public CollectionNode Collection;

        public ArticleNode(JSONObject entry)
        {
            try
            {
                Node = entry.getString("Node");

                Text = entry.getString("Text");

                if(Text.equals("null"))
                {
                    Text = null;
                }

                IsOptional = entry.getBoolean("IsOptional");

                boolean isItalics = false;

                try
                {
                    String isTmp = entry.getString("IsItalics");

                    if (isTmp.equals("true")) {
                        isItalics = true;
                    }
                }
                catch (JSONException e) {}

                boolean isAccent = false;

                try
                {
                    String isTmp = entry.getString("IsAccent");

                    if (isTmp.equals("true")) {
                        isAccent = true;
                    }
                }
                catch(JSONException e){}

                if(isComment())
                {
                    Markup = new CommentNode(entry.getJSONArray("Markup"));
                }
                else if(isParagraph())
                {
                    Markup = new ParagraphNode(entry.getJSONArray("Markup"));
                }
                else if(isText())
                {
                    Entry = new EntryNode(Text, isItalics, isAccent);
                }
                else if(isList())
                {
                    Collection = new CollectionNode(entry.getJSONArray("Items"));
                }
                else if(isListItem())
                {
                    Markup = new MarkupNode(entry.getJSONArray("Markup"));
                }
                else if(isExamples())
                {
                    Collection = new ExamplesListNode(entry.getJSONArray("Items"));
                }
                else if(isExampleItem())
                {
                    Markup = new ExampleItemNode(entry.getJSONArray("Markup"));
                }
                else if(isExample())
                {
                    Markup = new ExampleNode(entry.getJSONArray("Markup"));
                }
                else if(isCardRefs())
                {
                    Collection = new CardRefsListNode(entry.getJSONArray("Items"));
                }
                else if(isCardRefItem())
                {
                    Markup = new CardRefItemNode(entry.getJSONArray("Markup"));
                }
                else if(isCardRef())
                {
                    Entry = new CardRefNode(Text, isItalics, isAccent);
                }
                else if(isTranscription())
                {
                    Entry = new TranscriptionNode(Text);
                }
                else if(isAbbrev())
                {
                    Entry = new AbbrevNode(Text, entry.getString("FullText"));
                }
                else if(isCaption())
                {
                    Entry = new CaptionNode(Text);
                }
                else if(isSound())
                {
                    Entry = new SoundNode(entry.getString("FileName"));
                }
                else if(isRef())
                {
                    Entry = new EntryNode(Text, isItalics, isAccent);
                }

            }
            catch (JSONException e){}
        }

        public boolean isComment()
        {
            return Node.equals("Comment");
        }

        public boolean isParagraph()
        {
            return Node.equals("Paragraph");
        }

        public boolean isText()
        {
            return Node.equals("Text");
        }

        public boolean isList()
        {
            return Node.equals("List");
        }

        public boolean isListItem()
        {
            return Node.equals("ListItem");
        }

        public boolean isExamples()
        {
            return Node.equals("Examples");
        }

        public boolean isExampleItem()
        {
            return Node.equals("ExampleItem");
        }

        public boolean isExample()
        {
            return Node.equals("Example");
        }

        public boolean isCardRefs()
        {
            return Node.equals("CardRefs");
        }

        public boolean isCardRefItem()
        {
            return Node.equals("CardRefItem");
        }

        public boolean isCardRef()
        {
            return Node.equals("CardRef");
        }

        public boolean isTranscription()
        {
            return Node.equals("Transcription");
        }

        public boolean isAbbrev()
        {
            return Node.equals("Abbrev");
        }

        public boolean isCaption()
        {
            return Node.equals("Caption");
        }

        public boolean isSound()
        {
            return Node.equals("Sound");
        }

        public boolean isRef()
        {
            return Node.equals("Ref");
        }

        public String buildHtml()
        {
            String s = "";

            if(Entry != null)
            {
                s += Entry.buildHtml();
            }
            else if(Collection != null)
            {
                s += Collection.buildHtml();
            }
            else if(Markup != null)
            {
                s += Markup.buildHtml();
            }
            else
            {
                s += "<span>[" + Node + "]</span>";
            }

            return s;
        }
    }

    public class ArticleModel implements MarkupSequence
    {
        public String Title;
        public String Dictionary;
        public List<ArticleNode> Body = new ArrayList<>();

        public ArticleModel(JSONObject entry)
        {
            try {
                Title = entry.getString("Title");
                Dictionary = entry.getString("Dictionary");

                JSONArray bc = entry.getJSONArray("Body");

                int length = bc.length();

                for(int i = 0; i < length; ++i)
                {
                    Body.add(new ArticleNode(bc.getJSONObject(i)));
                }
            }
            catch (JSONException e){}
        }

        public String buildHtml()
        {
            String s = "<div class=\"dictionary\"><p class=\"headline\"><span class=\"word\">" + Title +
                        "</span><span>&nbsp;:&nbsp;</span><span class=\"book\">" + Dictionary + "</span></p>";

            int length = Body.size();

            for(int i = 0; i < length; ++i)
            {
                s += Body.get(i).buildHtml();
            }

            return s + "</div>";
        }
    }

    public class RootCollection
    {
        public List<ArticleModel> items = new ArrayList<>();
    }

    private class ParserTask extends AsyncTask<String, Integer, RootCollection>
    {
        private LingvoDictionaryEntryHandler handler;

        public ParserTask(LingvoDictionaryEntryHandler handler)
        {
            this.handler = handler;
        }

        protected RootCollection doInBackground(String... jsonItems)
        {
            String json = jsonItems[0];

            RootCollection collection = new RootCollection();

            try
            {
                JSONArray rc = new JSONArray(json);

                int length = rc.length();

                for(int i = 0; i < length; ++i)
                {
                    collection.items.add(new ArticleModel(rc.getJSONObject(i)));
                }
            }
            catch (JSONException e)
            {}

            return collection;
        }

        protected void onPostExecute(RootCollection result)
        {
            if(handler != null)
            {
                handler.consume(result);
            }
        }
    }

    private LingvoDictionaryEntryHandler handler;
    private ParserTask parserTask;

    public LingvoResponseParser(LingvoDictionaryEntryHandler handler)
    {
        parserTask = new ParserTask(handler);
    }

    public void parse(String json)
    {
        parserTask.execute(json);
    }
}
