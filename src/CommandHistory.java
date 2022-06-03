public class CommandHistory {
    private class Command{
        Command next,previous;
        String Text;
        public Command(String text){
            Text=text;
        }

        public void SetNext(Command nxt){
            next=nxt;
        }

        public void SetPrev(Command nxt){
            previous=nxt;
        }

        public void PrintLine(){
            System.out.println("|"+Text+"|");
            if(previous!=this){
                previous.PrintLine();
            }
        }
    }

    private Command head,tail,current;
    //head should always point to an empty string

    public void DebugPrint(){
        ;
        head.PrintLine();
    }

    public CommandHistory(){
        head=new Command("");
        head.SetPrev(head);
        head.SetNext(head);
        tail=head;

        current=head;
    }

    public String Oldest(){
        return tail.Text;
    }

    public String Newest(){
        return head.Text;
    }

    public String NewestReal(){
        return head.previous.Text;
    }

    public String Next(){
        current= current.next;
        return current.Text;
    }

    public String Previous(){
        current=current.previous;
        return current.Text;
    }

    public void Insert(String cmd){
        //insert between head and head.previous, unless head.previous==head, then the new.previous should be itself and head.previous should be head
        Command X=new Command(cmd);
        if(head.previous!=head){//regular insert
            X.SetPrev(head.previous);
            head.previous.SetNext(X);
        }
        else{//the first element -> set tail
            X.SetPrev(X);
            tail=X;
        }
        X.SetNext(head);
        head.SetPrev(X);
        current=head;
    }
}
