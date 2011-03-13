#include "comp_spawn_point.h"
#include "resources/res_spawn_point.h"

#include <string.h>

#include <dlib/array.h>
#include <dlib/dstrings.h>
#include <dlib/hash.h>
#include <dlib/index_pool.h>
#include <dlib/log.h>
#include <gameobject/gameobject.h>
#include <vectormath/cpp/vectormath_aos.h>

namespace dmGameSystem
{
    using namespace Vectormath::Aos;

    struct SpawnPointComponent
    {
        SpawnPointResource*     m_Resource;
        Vectormath::Aos::Point3 m_Position;
        Vectormath::Aos::Quat   m_Rotation;
        uint32_t                m_SpawnRequested : 1;
    };

    struct SpawnPointWorld
    {
        dmArray<SpawnPointComponent>    m_Components;
        dmIndexPool32                   m_IndexPool;
        uint32_t                        m_TotalSpawnCount;
    };

    dmGameObject::CreateResult CompSpawnPointNewWorld(void* context, void** world)
    {
        SpawnPointWorld* spw = new SpawnPointWorld();
        const uint32_t max_component_count = 64;
        spw->m_Components.SetCapacity(max_component_count); // TODO: Tweakable!
        spw->m_Components.SetSize(max_component_count);
        spw->m_IndexPool.SetCapacity(max_component_count);
        memset(&spw->m_Components[0], 0, sizeof(SpawnPointComponent) * max_component_count);
        *world = spw;
        return dmGameObject::CREATE_RESULT_OK;
    }

    dmGameObject::CreateResult CompSpawnPointDeleteWorld(void* context, void* world)
    {
        delete (SpawnPointWorld*)world;
        return dmGameObject::CREATE_RESULT_OK;
    }

    dmGameObject::CreateResult CompSpawnPointCreate(dmGameObject::HCollection collection,
                                               dmGameObject::HInstance instance,
                                               void* resource,
                                               void* world,
                                               void* context,
                                               uintptr_t* user_data)
    {
        SpawnPointWorld* spw = (SpawnPointWorld*)world;
        if (spw->m_IndexPool.Remaining() > 0)
        {
            uint32_t index = spw->m_IndexPool.Pop();
            SpawnPointComponent* spc = &spw->m_Components[index];
            spc->m_Resource = (SpawnPointResource*) resource;
            spc->m_SpawnRequested = 0;
            *user_data = (uintptr_t) spc;

            return dmGameObject::CREATE_RESULT_OK;
        }
        else
        {
            dmLogError("Can not create more spawn point components since the buffer is full (%d).", spw->m_Components.Size());
            return dmGameObject::CREATE_RESULT_UNKNOWN_ERROR;
        }
    }

    dmGameObject::CreateResult CompSpawnPointDestroy(dmGameObject::HCollection collection,
                                                dmGameObject::HInstance instance,
                                                void* world,
                                                void* context,
                                                uintptr_t* user_data)
    {
        SpawnPointWorld* spw = (SpawnPointWorld*)world;
        SpawnPointComponent* spc = (SpawnPointComponent*)*user_data;
        uint32_t index = spc - &spw->m_Components[0];
        spc->m_Resource = 0x0;
        spw->m_IndexPool.Push(index);
        return dmGameObject::CREATE_RESULT_OK;
    }

    dmGameObject::UpdateResult CompSpawnPointPostUpdate(dmGameObject::HCollection collection,
                                               void* world,
                                               void* context)
    {
        SpawnPointWorld* spw = (SpawnPointWorld*)world;
        const char* id_base = "spawn";
        char id[64];
        for (uint32_t i = 0; i < spw->m_Components.Size(); ++i)
        {
            SpawnPointComponent* spc = &spw->m_Components[i];
            if (spc->m_Resource != 0x0 && spc->m_SpawnRequested)
            {
                DM_SNPRINTF(id, 64, "%s%d", id_base, spw->m_TotalSpawnCount);
                dmGameObject::Spawn(collection, spc->m_Resource->m_SpawnPointDesc->m_Prototype, id, spc->m_Position, spc->m_Rotation);
                spc->m_SpawnRequested = 0;
                ++spw->m_TotalSpawnCount;
            }
        }
        return dmGameObject::UPDATE_RESULT_OK;
    }

    dmGameObject::UpdateResult CompSpawnPointOnMessage(dmGameObject::HInstance instance,
                                                const dmGameObject::InstanceMessageData* message_data,
                                                void* context,
                                                uintptr_t* user_data)
    {
        if (message_data->m_MessageId == dmHashString64("spawn_object"))
        {
            SpawnPointComponent* spc = (SpawnPointComponent*) *user_data;
            if (message_data->m_DDFDescriptor == dmGameSystemDDF::SpawnObject::m_DDFDescriptor)
            {
                if (spc->m_SpawnRequested)
                {
                    dmLogError("%s", "It is only allowed to spawn once per spawn point per frame, request ignored.");
                }
                else
                {
                    dmGameSystemDDF::SpawnObject* spawn_object = (dmGameSystemDDF::SpawnObject*) message_data->m_Buffer;
                    spc->m_SpawnRequested = 1;
                    spc->m_Position = spawn_object->m_Position;
                    spc->m_Rotation = spawn_object->m_Rotation;
                }
            }
            else
            {
                dmLogError("Invalid DDF-type for spawn_object message");
                return dmGameObject::UPDATE_RESULT_UNKNOWN_ERROR;
            }
        }
        return dmGameObject::UPDATE_RESULT_OK;
    }
}
