#include "script_util.h"
#include <string.h>
extern "C"
{
#include <lua/lua.h>
#include <lua/lauxlib.h>
}

namespace dmScriptUtil
{
    static void DoLuaTableToDDF(lua_State* L, const dmDDF::Descriptor* descriptor,
                                char* buffer, char** data_start, char** data_last);

    static void LuaValueToDDF(lua_State* L, const dmDDF::FieldDescriptor* f,
                              char* buffer, char** data_start, char** data_end)
    {
    	bool nil_val = lua_isnil(L, -1);
        switch (f->m_Type)
        {
            case dmDDF::TYPE_INT32:
            {
            	if (nil_val)
            		*((int32_t *) &buffer[f->m_Offset]) = 0;
            	else
            		*((int32_t *) &buffer[f->m_Offset]) = (int32_t) luaL_checkinteger(L, -1);
            }
            break;

            case dmDDF::TYPE_UINT32:
            {
            	if (nil_val)
            		*((uint32_t *) &buffer[f->m_Offset]) = 0;
            	else
            		*((uint32_t *) &buffer[f->m_Offset]) = (uint32_t) luaL_checkinteger(L, -1);
            }
            break;

            case dmDDF::TYPE_FLOAT:
            {
            	if (nil_val)
            		*((float *) &buffer[f->m_Offset]) = 0.0f;
            	else
            		*((float *) &buffer[f->m_Offset]) = (float) luaL_checknumber(L, -1);
            }
            break;

            case dmDDF::TYPE_STRING:
            {
                const char* s = "";
                if (!nil_val)
                	s = luaL_checkstring(L, -1);
                int size = strlen(s) + 1;
                if (*data_start + size > *data_end)
                {
                    luaL_error(L, "Event data doesn't fit");
                }
                else
                {
                    memcpy(*data_start, s, size);
                    // NOTE: We store offset here an relocate later...
                    *((const char**) &buffer[f->m_Offset]) = (const char*) (*data_start - buffer);
                }
                *data_start += size;
            }
            break;

            case dmDDF::TYPE_MESSAGE:
            {
            	if (!nil_val)
            	{
					const dmDDF::Descriptor* d = f->m_MessageDescriptor;
					DoLuaTableToDDF(L, d, &buffer[f->m_Offset], data_start, data_end);
            	}
            }
            break;

            default:
            {
                luaL_error(L, "Unsupported type %d in field %s", f->m_Type, f->m_Name);
            }
            break;
        }
    }

    static void DoLuaTableToDDF(lua_State* L, const dmDDF::Descriptor* descriptor,
                                char* buffer, char** data_start, char** data_last)
    {
        luaL_checktype(L, -1, LUA_TTABLE);

        for (uint32_t i = 0; i < descriptor->m_FieldCount; ++i)
        {
            const dmDDF::FieldDescriptor* f = &descriptor->m_Fields[i];

            lua_pushstring(L, f->m_Name);
            lua_rawget(L, -2);
            if (lua_isnil(L, -1) && f->m_Label != dmDDF::LABEL_OPTIONAL)
            {
                luaL_error(L, "Field %s not specified in table", f->m_Name);
            }
            else
            {
            	LuaValueToDDF(L, f, buffer, data_start, data_last);
            }
            lua_pop(L, 1);
        }
    }

    void LuaTableToDDF(lua_State* L, const dmDDF::Descriptor* descriptor, char* buffer, uint32_t buffer_size)
    {
        uint32_t size = descriptor->m_Size;

        if (size > buffer_size)
        {
            luaL_error(L, "sizeof(%s) > %d", descriptor->m_Name, buffer_size);
        }

        char* data_start = buffer + size;
        char* data_end = data_start + buffer_size - size;

        DoLuaTableToDDF(L, descriptor, buffer, &data_start, &data_end);
    }

    void DDFToLuaValue(lua_State* L, const dmDDF::FieldDescriptor* f, const char* data)
    {
        switch (f->m_Type)
        {
            case dmDDF::TYPE_INT32:
            {
                lua_pushinteger(L, (int) *((int32_t*) &data[f->m_Offset]));
            }
            break;

            case dmDDF::TYPE_UINT32:
            {
                lua_pushinteger(L, (int) *((uint32_t*) &data[f->m_Offset]));
            }
            break;

            case dmDDF::TYPE_FLOAT:
            {
                lua_pushnumber(L, (lua_Number) *((float*) &data[f->m_Offset]));
            }
            break;

            case dmDDF::TYPE_STRING:
            {
                lua_pushstring(L, *((const char**) &data[f->m_Offset]));
            }
            break;

            case dmDDF::TYPE_MESSAGE:
            {
                const dmDDF::Descriptor* d = f->m_MessageDescriptor;

                lua_newtable(L);
                for (uint32_t i = 0; i < d->m_FieldCount; ++i)
                {
                    const dmDDF::FieldDescriptor* f2 = &d->m_Fields[i];
                    lua_pushstring(L, f2->m_Name);
                    DDFToLuaValue(L, &d->m_Fields[i], &data[f->m_Offset]);
                    lua_rawset(L, -3);
                }
            }
            break;

            default:
            {
                luaL_error(L, "Unsupported type %d in field %s", f->m_Type, f->m_Name);
            }
        }
    }

    void DDFToLuaTable(lua_State*L, const dmDDF::Descriptor* d, const char* data)
    {
        lua_newtable(L);
        for (uint32_t i = 0; i < d->m_FieldCount; ++i)
        {
            const dmDDF::FieldDescriptor* f = &d->m_Fields[i];

            lua_pushstring(L, f->m_Name);
            DDFToLuaValue(L, &d->m_Fields[i], data);
            lua_rawset(L, -3);
        }
    }
}
