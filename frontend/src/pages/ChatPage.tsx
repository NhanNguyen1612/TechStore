import { Client } from "@stomp/stompjs";
import clsx from "clsx";
import {
  ImagePlus,
  LoaderCircle,
  MessageCircle,
  Search,
  Send,
  UserRound,
  UsersRound,
  Wifi,
  WifiOff,
} from "lucide-react";
import {
  type FormEvent,
  useCallback,
  useEffect,
  useMemo,
  useRef,
  useState,
} from "react";
import SockJS from "sockjs-client";
import { EmptyState } from "../components/EmptyState";
import { api, apiData } from "../lib/api";
import { authStorage } from "../lib/authStorage";
import { formatDate, getErrorMessage } from "../lib/format";
import type {
  ApiResponse,
  ChatContact,
  ChatEvent,
  ChatMessage,
  ChatParticipant,
  Conversation,
  PageResponse,
  Role,
} from "../types/api";

const roleLabels: Record<Role, string> = {
  ROLE_ADMIN: "Quản trị",
  ROLE_STAFF: "Nhân viên",
  ROLE_CUSTOMER: "Khách hàng",
};

export function ChatPage() {
  const clientRef = useRef<Client | null>(null);
  const activeIdRef = useRef<number | null>(null);
  const initialConversationLoadedRef = useRef(false);
  const messagesEndRef = useRef<HTMLDivElement | null>(null);
  const [conversations, setConversations] = useState<Conversation[]>([]);
  const [contacts, setContacts] = useState<ChatContact[]>([]);
  const [activeId, setActiveId] = useState<number | null>(null);
  const [selectedContact, setSelectedContact] = useState<ChatContact | null>(
    null,
  );
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [message, setMessage] = useState("");
  const [search, setSearch] = useState("");
  const [contactsLoading, setContactsLoading] = useState(true);
  const [connected, setConnected] = useState(false);
  const [error, setError] = useState("");

  const loadConversations = useCallback(async () => {
    try {
      const page = await apiData<PageResponse<Conversation>>(
        api.get("/api/conversations", { params: { size: 100 } }),
      );
      const sorted = [...page.content].sort(
        (left, right) =>
          conversationTime(right) - conversationTime(left),
      );
      setConversations(sorted);
      if (!initialConversationLoadedRef.current) {
        initialConversationLoadedRef.current = true;
        setActiveId(sorted[0]?.id ?? null);
      }
    } catch (requestError) {
      setError(getErrorMessage(requestError));
    }
  }, []);

  const loadContacts = useCallback(async (query: string) => {
    setContactsLoading(true);
    try {
      const page = await apiData<PageResponse<ChatContact>>(
        api.get("/api/chat/contacts", {
          params: { search: query.trim(), size: 100 },
        }),
      );
      setContacts(page.content);
    } catch (requestError) {
      setError(getErrorMessage(requestError));
    } finally {
      setContactsLoading(false);
    }
  }, []);

  const loadMessages = useCallback(async (conversationId: number) => {
    try {
      const page = await apiData<PageResponse<ChatMessage>>(
        api.get(`/api/messages/${conversationId}`, {
          params: { size: 100 },
        }),
      );
      setMessages([...page.content].reverse());
      await api.put(`/api/messages/${conversationId}/read`);
      setConversations((current) =>
        current.map((conversation) =>
          conversation.id === conversationId
            ? { ...conversation, unreadCount: 0 }
            : conversation,
        ),
      );
    } catch (requestError) {
      setError(getErrorMessage(requestError));
    }
  }, []);

  useEffect(() => {
    loadConversations();
  }, [loadConversations]);

  useEffect(() => {
    const timeout = window.setTimeout(() => loadContacts(search), 250);
    return () => window.clearTimeout(timeout);
  }, [loadContacts, search]);

  useEffect(() => {
    if (activeId) {
      loadMessages(activeId);
    } else {
      setMessages([]);
    }
  }, [activeId, loadMessages]);

  useEffect(() => {
    activeIdRef.current = activeId;
  }, [activeId]);

  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: "smooth" });
  }, [messages]);

  useEffect(() => {
    const apiUrl = import.meta.env.VITE_API_URL?.replace(/\/$/, "") ?? "";
    const client = new Client({
      reconnectDelay: 4000,
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000,
      webSocketFactory: () => new SockJS(`${apiUrl}/ws`),
      beforeConnect: async () => {
        const token = authStorage.getAccessToken();
        client.connectHeaders = token
          ? { Authorization: `Bearer ${token}` }
          : {};
      },
      onConnect: () => {
        setConnected(true);
        client.subscribe("/user/queue/chat/private", (frame) => {
          handleEvent(JSON.parse(frame.body) as ChatEvent);
        });
        client.subscribe("/user/queue/chat/errors", (frame) => {
          const response = JSON.parse(frame.body) as ApiResponse<unknown>;
          setError(response.message);
        });
      },
      onDisconnect: () => setConnected(false),
      onWebSocketClose: () => setConnected(false),
      onStompError: (frame) => setError(frame.headers.message || "Lỗi trò chuyện"),
    });
    clientRef.current = client;
    client.activate();
    return () => {
      client.deactivate();
      clientRef.current = null;
    };
  }, []);

  const handleEvent = (event: ChatEvent) => {
    if (event.eventType === "MESSAGE" && event.message) {
      const incoming = event.message;
      setMessages((current) =>
        incoming.conversationId === activeIdRef.current &&
        !current.some((item) => item.id === incoming.id)
          ? [...current, incoming]
          : current,
      );
      setActiveId((current) => current ?? incoming.conversationId);
      setSelectedContact(null);
      loadConversations();
      if (
        !incoming.mine &&
        incoming.conversationId === activeIdRef.current
      ) {
        api.put(`/api/messages/${incoming.conversationId}/read`);
      }
    }
    if (event.eventType === "READ" && event.readReceipt) {
      setMessages((current) =>
        current.map((item) =>
          item.mine
            ? { ...item, read: true, readAt: event.readReceipt?.readAt }
            : item,
        ),
      );
    }
  };

  const activeConversation = conversations.find(
    (conversation) => conversation.id === activeId,
  );
  const participant = activeConversation?.participant ?? selectedContact;
  const conversationParticipantIds = useMemo(
    () =>
      new Set(
        conversations.map((conversation) => conversation.participant.id),
      ),
    [conversations],
  );
  const newContacts = contacts.filter(
    (contact) => !conversationParticipantIds.has(contact.id),
  );

  const selectConversation = (conversation: Conversation) => {
    setSelectedContact(null);
    setActiveId(conversation.id);
  };

  const selectContact = (contact: ChatContact) => {
    const existing = conversations.find(
      (conversation) => conversation.participant.id === contact.id,
    );
    if (existing) {
      selectConversation(existing);
      return;
    }
    setActiveId(null);
    setSelectedContact(contact);
    setMessages([]);
  };

  const sendText = (event: FormEvent) => {
    event.preventDefault();
    const content = message.trim();
    if (!content || !participant || !clientRef.current?.connected) return;
    clientRef.current.publish({
      destination: "/chat/private",
      body: JSON.stringify({
        conversationId: activeId,
        recipientId: activeId ? null : participant.id,
        type: "TEXT",
        content,
      }),
    });
    setMessage("");
  };

  const sendImage = async (file: File) => {
    if (!participant) return;
    try {
      const form = new FormData();
      form.append("image", file);
      const params = activeId
        ? { conversationId: activeId }
        : { recipientId: participant.id };
      const sent = await apiData<ChatMessage>(
        api.post("/api/messages/image", form, {
          params,
        }),
      );
      setSelectedContact(null);
      setActiveId(sent.conversationId);
      await loadConversations();
    } catch (requestError) {
      setError(getErrorMessage(requestError));
    }
  };

  return (
    <section className="container-page py-8 sm:py-12">
      <div className="mb-6 flex flex-wrap items-end justify-between gap-3">
        <div>
          <p className="text-sm font-bold uppercase tracking-[0.2em] text-teal">
            Hỗ trợ trực tuyến
          </p>
          <h1 className="section-title mt-2">Tin nhắn</h1>
        </div>
        <span
          className={clsx(
            "inline-flex items-center gap-2 rounded-full px-3 py-2 text-xs font-bold",
            connected ? "bg-lime" : "bg-red-100 text-red-700",
          )}
        >
          {connected ? (
            <Wifi className="h-4 w-4" />
          ) : (
            <WifiOff className="h-4 w-4" />
          )}
          {connected ? "Đã kết nối" : "Đang kết nối lại"}
        </span>
      </div>

      {error && (
        <button
          className="mb-4 w-full rounded-2xl bg-red-50 p-3 text-left text-sm text-red-700"
          onClick={() => setError("")}
        >
          {error}
        </button>
      )}

      <div className="card grid min-h-[650px] overflow-hidden lg:grid-cols-[350px_1fr]">
        <aside className="border-b border-ink/5 bg-cream/50 lg:border-b-0 lg:border-r">
          <div className="border-b border-ink/5 p-4">
            <label htmlFor="chat-search" className="label">
              Tìm người dùng
            </label>
            <div className="relative">
              <Search className="pointer-events-none absolute left-4 top-1/2 h-4 w-4 -translate-y-1/2 text-ink/40" />
              <input
                id="chat-search"
                type="search"
                className="field pl-11"
                value={search}
                maxLength={100}
                onChange={(event) => setSearch(event.target.value)}
                placeholder="Tìm theo tên hoặc email..."
              />
            </div>
          </div>

          <div className="max-h-[420px] overflow-y-auto lg:max-h-[570px]">
            {search.trim() ? (
              <ContactResults
                contacts={contacts}
                loading={contactsLoading}
                selectedId={participant?.id}
                onSelect={selectContact}
              />
            ) : (
              <>
                {conversations.length > 0 && (
                  <div>
                    <ListHeading>Trò chuyện gần đây</ListHeading>
                    {conversations.map((conversation) => (
                      <ConversationButton
                        key={conversation.id}
                        conversation={conversation}
                        active={activeId === conversation.id}
                        onClick={() => selectConversation(conversation)}
                      />
                    ))}
                  </div>
                )}

                <div>
                  <ListHeading>Bắt đầu cuộc trò chuyện mới</ListHeading>
                  {contactsLoading ? (
                    <LoadingContacts />
                  ) : newContacts.length > 0 ? (
                    newContacts.map((contact) => (
                      <ContactButton
                        key={contact.id}
                        contact={contact}
                        active={
                          !activeId && selectedContact?.id === contact.id
                        }
                        onClick={() => selectContact(contact)}
                      />
                    ))
                  ) : (
                    <p className="px-4 py-6 text-center text-sm text-ink/45">
                      Tất cả người dùng khả dụng đã có trong danh sách trò chuyện gần đây.
                    </p>
                  )}
                </div>
              </>
            )}
          </div>
        </aside>

        <div className="flex min-h-[520px] flex-col">
          {participant && (
            <div className="flex items-center gap-3 border-b border-ink/5 px-4 py-3 sm:px-6">
              <Avatar participant={participant} size="large" />
              <div className="min-w-0">
                <p className="truncate font-bold">{participant.fullName}</p>
                <p className="text-xs text-ink/45">
                  {roleLabels[participant.role]}
                  {"email" in participant && participant.email
                    ? ` · ${participant.email}`
                    : ""}
                </p>
              </div>
            </div>
          )}

          <div className="flex-1 overflow-y-auto p-4 sm:p-6">
            {!participant ? (
              <EmptyState
                icon={MessageCircle}
                title="Chọn một người để nhắn tin"
                description="Chọn cuộc trò chuyện gần đây hoặc tìm người dùng theo tên."
              />
            ) : !activeId && messages.length === 0 ? (
              <EmptyState
                icon={UsersRound}
                title={`Trò chuyện với ${participant.fullName}`}
                description="Gửi tin nhắn đầu tiên để bắt đầu cuộc trò chuyện."
              />
            ) : (
              <div className="grid gap-4">
                {messages.map((item) => (
                  <div
                    key={item.id}
                    className={clsx(
                      "flex",
                      item.mine ? "justify-end" : "justify-start",
                    )}
                  >
                    <div
                      className={clsx(
                        "max-w-[82%] rounded-3xl px-4 py-3 sm:max-w-[70%]",
                        item.mine
                          ? "rounded-br-md bg-ink text-white"
                          : "rounded-bl-md bg-cream text-ink",
                      )}
                    >
                      {item.type === "IMAGE" ? (
                        <img
                          src={item.content}
                          alt="Tệp đính kèm trò chuyện"
                          className="max-h-80 rounded-2xl object-cover"
                        />
                      ) : (
                        <p className="whitespace-pre-wrap text-sm leading-6">
                          {item.content}
                        </p>
                      )}
                      <p
                        className={clsx(
                          "mt-2 text-[10px]",
                          item.mine ? "text-white/45" : "text-ink/40",
                        )}
                      >
                        {formatDate(item.createdAt)}
                        {item.mine && (item.read ? " · Đã đọc" : " · Đã gửi")}
                      </p>
                    </div>
                  </div>
                ))}
                <div ref={messagesEndRef} />
              </div>
            )}
          </div>

          <form
            onSubmit={sendText}
            className="flex items-center gap-2 border-t border-ink/5 p-3 sm:p-4"
          >
            <label
              className={clsx(
                "cursor-pointer rounded-full p-3 hover:bg-cream",
                !participant && "pointer-events-none opacity-35",
              )}
              aria-label="Gửi hình ảnh"
            >
              <ImagePlus className="h-5 w-5" />
              <input
                type="file"
                accept="image/jpeg,image/png,image/webp"
                className="hidden"
                disabled={!participant}
                onChange={(event) => {
                  const file = event.target.files?.[0];
                  if (file) sendImage(file);
                  event.currentTarget.value = "";
                }}
              />
            </label>
            <input
              className="field"
              value={message}
              maxLength={2000}
              disabled={!participant}
              onChange={(event) => setMessage(event.target.value)}
              placeholder={
                participant
                  ? `Nhắn tin cho ${participant.fullName}...`
                  : "Chọn người để bắt đầu trò chuyện"
              }
            />
            <button
              disabled={!connected || !participant || !message.trim()}
              className="rounded-full bg-ink p-3.5 text-lime disabled:opacity-40"
              aria-label="Gửi tin nhắn"
            >
              <Send className="h-5 w-5" />
            </button>
          </form>
        </div>
      </div>
    </section>
  );
}

function ConversationButton({
  conversation,
  active,
  onClick,
}: {
  conversation: Conversation;
  active: boolean;
  onClick: () => void;
}) {
  return (
    <button
      type="button"
      onClick={onClick}
      className={clsx(
        "flex w-full items-center gap-3 border-b border-ink/5 p-4 text-left transition",
        active ? "bg-white" : "hover:bg-white/60",
      )}
    >
      <Avatar participant={conversation.participant} />
      <span className="min-w-0 flex-1">
        <span className="block truncate font-bold">
          {conversation.participant.fullName}
        </span>
        <span className="mt-1 block truncate text-xs text-ink/45">
          {conversation.lastMessage?.type === "IMAGE"
            ? "Đã gửi một hình ảnh"
            : conversation.lastMessage?.content || "Cuộc trò chuyện mới"}
        </span>
      </span>
      {conversation.unreadCount > 0 && (
        <span className="grid h-6 min-w-6 place-items-center rounded-full bg-coral px-1 text-xs font-bold text-white">
          {conversation.unreadCount}
        </span>
      )}
    </button>
  );
}

function ContactResults({
  contacts,
  loading,
  selectedId,
  onSelect,
}: {
  contacts: ChatContact[];
  loading: boolean;
  selectedId?: number;
  onSelect: (contact: ChatContact) => void;
}) {
  if (loading) return <LoadingContacts />;
  if (contacts.length === 0) {
    return (
      <p className="px-4 py-10 text-center text-sm text-ink/45">
        Không tìm thấy người dùng. Hãy thử tên hoặc email khác.
      </p>
    );
  }
  return (
    <div>
      <ListHeading>Mọi người</ListHeading>
      {contacts.map((contact) => (
        <ContactButton
          key={contact.id}
          contact={contact}
          active={selectedId === contact.id}
          onClick={() => onSelect(contact)}
        />
      ))}
    </div>
  );
}

function ContactButton({
  contact,
  active,
  onClick,
}: {
  contact: ChatContact;
  active: boolean;
  onClick: () => void;
}) {
  return (
    <button
      type="button"
      onClick={onClick}
      className={clsx(
        "flex w-full items-center gap-3 border-b border-ink/5 p-4 text-left transition",
        active ? "bg-white" : "hover:bg-white/60",
      )}
    >
      <Avatar participant={contact} />
      <span className="min-w-0 flex-1">
        <span className="block truncate font-bold">{contact.fullName}</span>
        <span className="mt-1 block truncate text-xs text-ink/45">
          {contact.email}
        </span>
      </span>
      <span className="rounded-full bg-lime/45 px-2 py-1 text-[10px] font-extrabold uppercase">
        {roleLabels[contact.role]}
      </span>
    </button>
  );
}

function Avatar({
  participant,
  size = "normal",
}: {
  participant: ChatParticipant;
  size?: "normal" | "large";
}) {
  const sizeClass = size === "large" ? "h-12 w-12" : "h-11 w-11";
  if (participant.avatarUrl) {
    return (
      <img
        src={participant.avatarUrl}
        alt=""
        className={clsx(sizeClass, "shrink-0 rounded-full object-cover")}
      />
    );
  }
  return (
    <span
      className={clsx(
        sizeClass,
        "grid shrink-0 place-items-center rounded-full bg-lime",
      )}
    >
      <UserRound className="h-5 w-5" />
    </span>
  );
}

function ListHeading({ children }: { children: string }) {
  return (
    <p className="px-4 pb-2 pt-4 text-[11px] font-extrabold uppercase tracking-[0.16em] text-ink/40">
      {children}
    </p>
  );
}

function LoadingContacts() {
  return (
    <div className="flex items-center justify-center gap-2 px-4 py-10 text-sm text-ink/45">
      <LoaderCircle className="h-4 w-4 animate-spin" />
      Đang tải danh sách người dùng...
    </div>
  );
}

function conversationTime(conversation: Conversation) {
  return new Date(
    conversation.lastMessageAt ?? conversation.updatedAt,
  ).getTime();
}
